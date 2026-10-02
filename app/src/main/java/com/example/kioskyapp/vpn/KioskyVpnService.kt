package com.example.kioskyapp.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.kioskyapp.R
import com.example.kioskyapp.apiServices.WebFilterApi
import com.example.kioskyapp.data.Result
import com.example.kioskyapp.models.FilterType
import com.example.kioskyapp.utils.KioskySettings
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext

class KioskyVpnService : VpnService() {
    companion object {
        const val ACTION_STOP = "com.example.kioskyapp.STOP_VPN"
        private const val PREFS_NAME = "kiosky_prefs"
        private const val VPN_RUNNING_PREF_KEY = "vpn_service_running"
        private const val VPN_STATUS_MESSAGE_PREF_KEY = "vpn_status_message"
        private const val CHANNEL_ID = "kioskyapp_vpn"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())
    private var vpnInterface: ParcelFileDescriptor? = null
    private var syncJob: Job? = null
    private var workerJob: Job? = null

    private lateinit var settings: KioskySettings
    private val api = WebFilterApi()
    
    private val blockedDomains = mutableSetOf<String>()
    private var safeSearchEnabled = false
    private var youtubeSafeSearchEnabled = false

    private val NOTIFICATION_ID = 4567
    private val FILTER_SYNC_INTERVAL_MS = 10_000L
    private val BLOCKED_WEBSITE_CACHE_KEY = "blocked_website_domains_cache"

    // Safe Search VIPs
    private val BLOCK_IP = "127.0.0.1"
    private val GOOGLE_SAFE_SEARCH_HOST = "forcesafesearch.google.com"
    private val YOUTUBE_RESTRICT_HOST = "restrict.youtube.com"
    private val DOH_BLOCKLIST = setOf(
        "dns.google",
        "dns.google.com",
        "cloudflare-dns.com",
        "mozilla.cloudflare-dns.com",
        "family.cloudflare-dns.com",
        "security.cloudflare-dns.com",
        "dns.quad9.net",
        "doh.opendns.com",
        "doh.cleanbrowsing.org",
        "adult-filter-dns.cleanbrowsing.org",
        "family-filter-dns.cleanbrowsing.org"
    )
    private val SAFE_SEARCH_ADULT_DOMAIN_KEYWORDS = setOf(
        "pornhub", "xnxx", "xvideos", "xvideo", "youporn", "redtube",
        "xhamster", "spankbang", "tube8", "tnaflix", "beeg", "brazzers", "onlyfans"
    )

    override fun onCreate() {
        super.onCreate()
        settings = KioskySettings(applicationContext)
        ensureNotificationChannel()
        loadCachedBlockedDomains()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }

        return try {
            ensureNotificationChannel()
            startVpnInForeground()

            if (startVpn()) {
                setVpnStatusMessage(null)
                START_STICKY
            } else {
                stopSelf()
                START_NOT_STICKY
            }
        } catch (e: Exception) {
            Log.e("KioskyVpn", "Failed to start foreground service", e)
            handleVpnFailure("kioskyApp could not start web protection. Please check VPN settings and try again.")
            stopSelf()
            START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        stopVpn()
        scope.cancel()
        super.onDestroy()
    }

    override fun onRevoke() {
        Log.w("KioskyVpn", "VPN permission revoked or another VPN became active")
        handleVpnFailure("kioskyApp web protection stopped because another VPN became active. Android only allows one VPN at a time.")
        stopVpn()
        stopSelf()
        super.onRevoke()
    }

    private fun startVpn(): Boolean {
        stopVpn(removeForeground = false)

        val childToken = settings.getString("child_token")
        val childId = settings.getString("child_id")

        if (childToken.isNullOrBlank() || childId.isNullOrBlank()) {
            Log.e("KioskyVpn", "Missing credentials")
            handleVpnFailure("kioskyApp needs a valid child session before web protection can start.")
            setVpnRunning(false)
            stopSelf()
            return false
        }

        loadCachedBlockedDomains()

        try {
            val builder = Builder()
                .setSession("kioskyApp Web Filter")
                .setMtu(1500)
                .addAddress("10.0.0.2", 32)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                .addRoute("1.1.1.1", 32)
                .addRoute("1.0.0.1", 32)
                .addRoute("8.8.8.8", 32)
                .addRoute("8.8.4.4", 32)
                .addRoute("9.9.9.9", 32)
                .addDisallowedApplication(packageName)

            vpnInterface = builder.establish()

            if (vpnInterface == null) {
                Log.e("KioskyVpn", "VPN establish() returned null (permission missing or config invalid)")
                handleVpnFailure("kioskyApp could not establish the VPN tunnel. Please try enabling web protection again.")
                setVpnRunning(false)
                stopVpn()
                stopSelf()
                return false
            }

            syncJob = scope.launch {
                while (isActive) {
                    fetchSyncData(childToken, childId)
                    delay(FILTER_SYNC_INTERVAL_MS)
                }
            }

            workerJob = scope.launch { processPackets() }
            setVpnRunning(true)
            Log.d("KioskyVpn", "VPN Started")
            return true
        } catch (e: Exception) {
            Log.e("KioskyVpn", "VPN Start failed", e)
            handleVpnFailure(
                if (e is SecurityException) {
                    "kioskyApp lost VPN permission. Please allow VPN access again."
                } else {
                    "kioskyApp could not start web protection on this device. Please try again."
                }
            )
            setVpnRunning(false)
            stopVpn()
            stopSelf()
            return false
        }
    }

    private suspend fun fetchSyncData(token: String, childId: String) {
        try {
            val filterResult = api.getContentFilters(token, childId)
            if (filterResult is Result.Success) {
                // 1. Sync Forbidden Websites
                val newBlocked = filterResult.data
                    .filter {
                        (it.filter_type == FilterType.WEBSITE || it.filter_type == FilterType.KEYWORD) &&
                            it.is_blocked
                    }
                    .map { normalizeDomain(it.value) }
                    .filter { it.isNotBlank() }
                    .toSet()

                synchronized(blockedDomains) {
                    blockedDomains.clear()
                    blockedDomains.addAll(newBlocked)
                }
                settings.putString(BLOCKED_WEBSITE_CACHE_KEY, newBlocked.sorted().joinToString(","))

                // 2. Sync Category Settings (Safe Search etc.)
                val categories = filterResult.data.filter { it.filter_type == FilterType.CATEGORY }
                safeSearchEnabled = categories.any { it.value == "SAFE_SEARCH" && it.is_blocked }
                youtubeSafeSearchEnabled = categories.any { it.value == "YOUTUBE_SAFE_SEARCH" && it.is_blocked }
                
                Log.d("KioskyVpn", "Sync OK: Domains=${blockedDomains.size}, Safe=$safeSearchEnabled, YT=$youtubeSafeSearchEnabled")
            }
        } catch (e: Exception) {
            Log.e("KioskyVpn", "Sync Error", e)
        }
    }

    private fun loadCachedBlockedDomains() {
        val cached = settings.getString(BLOCKED_WEBSITE_CACHE_KEY, "")
            ?.split(",")
            ?.map { normalizeDomain(it) }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            .orEmpty()

        if (cached.isEmpty()) return

        synchronized(blockedDomains) {
            if (blockedDomains.isEmpty()) {
                blockedDomains.addAll(cached)
            }
        }
    }

    private fun normalizeDomain(value: String): String {
        var normalized = value.trim().lowercase()
        if (normalized.isBlank()) return ""

        normalized = normalized.removePrefix("view-source:")
        normalized = normalized.substringAfter("://", normalized)
        normalized = normalized.substringAfter("@", normalized)
        normalized = normalized.substringBefore('/')
        normalized = normalized.substringBefore('?')
        normalized = normalized.substringBefore('#')
        normalized = normalized.substringBefore(':')
        normalized = normalized.removePrefix("www.")
        normalized = normalized.removePrefix("*.")
        return normalized.trim('.')
    }

    private fun isKeywordStyleRule(rule: String): Boolean {
        return rule.isNotBlank() && !rule.contains('.')
    }

    private fun matchesBlockedRule(domain: String, rule: String): Boolean {
        val normalizedDomain = normalizeDomain(domain)
        val normalizedRule = normalizeDomain(rule)

        if (normalizedDomain.isBlank() || normalizedRule.isBlank()) {
            return false
        }

        return if (isKeywordStyleRule(normalizedRule)) {
            normalizedDomain.contains(normalizedRule)
        } else {
            normalizedDomain == normalizedRule || normalizedDomain.endsWith(".$normalizedRule")
        }
    }

    private suspend fun processPackets() {
        val fileDescriptor = vpnInterface?.fileDescriptor
        if (fileDescriptor == null) {
            Log.e("KioskyVpn", "processPackets called without VPN interface")
            return
        }

        val input = FileInputStream(fileDescriptor)
        val output = FileOutputStream(fileDescriptor)
        val packet = ByteBuffer.allocate(32767)

        try {
            while (coroutineContext.isActive) {
                val length = input.read(packet.array())
                if (length > 0) {
                    packet.limit(length)
                    if (isDnsQuery(packet)) {
                        handleDnsPacket(packet, length, output)
                    } else {
                        // We only handle DNS. Anything else must be forwarded (not implemented), so drop it.
                    }
                    packet.clear()
                }
            }
        } catch (e: Exception) {
            Log.e("KioskyVpn", "Packet processing loop failed", e)
        } finally {
            try {
                input.close()
            } catch (_: Exception) {
            }
            try {
                output.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun handleDnsPacket(packet: ByteBuffer, length: Int, output: FileOutputStream) {
        val domain = getDnsDomain(packet) ?: return
        val qtype = getDnsQuestionType(packet)
        
        // 1. Check blocked list
        val isBlocked = synchronized(blockedDomains) {
            blockedDomains.any { matchesBlockedRule(domain, it) }
        }

        if (safeSearchEnabled && isDoHDomain(domain)) {
            Log.i("KioskyVpn", "BLOCK_DOH: $domain qtype=$qtype")
            val spoofedResponse = when (qtype) {
                28 -> constructSpoofedDnsResponse(packet, recordType = 28, ipBytes = InetAddress.getByName("::1").address)
                else -> constructSpoofedDnsResponse(packet, recordType = 1, ipBytes = InetAddress.getByName(BLOCK_IP).address)
            }
            try {
                synchronized(output) { output.write(spoofedResponse) }
            } catch (e: Exception) {
                Log.e("KioskyVpn", "Failed to write DoH block response", e)
            }
            return
        }

        if (safeSearchEnabled && matchesSafeSearchAdultDomain(domain)) {
            Log.i("KioskyVpn", "SAFE_SEARCH_BLOCK: $domain qtype=$qtype")
            val spoofedResponse = when (qtype) {
                28 -> constructSpoofedDnsResponse(packet, recordType = 28, ipBytes = InetAddress.getByName("::1").address)
                else -> constructSpoofedDnsResponse(packet, recordType = 1, ipBytes = InetAddress.getByName(BLOCK_IP).address)
            }
            try {
                synchronized(output) { output.write(spoofedResponse) }
            } catch (e: Exception) {
                Log.e("KioskyVpn", "Failed to write safe search site block response", e)
            }
            return
        }

        if (isBlocked) {
            Log.i("KioskyVpn", "BLOCKED: $domain qtype=$qtype")
            val spoofedResponse = when (qtype) {
                28 -> constructSpoofedDnsResponse(packet, recordType = 28, ipBytes = InetAddress.getByName("::1").address)
                else -> constructSpoofedDnsResponse(packet, recordType = 1, ipBytes = InetAddress.getByName(BLOCK_IP).address)
            }
            try {
                synchronized(output) { output.write(spoofedResponse) }
            } catch (e: Exception) {
                Log.e("KioskyVpn", "Failed to write spoofed response", e)
            }
            return
        }

        // 2. Safe Search Spoofing
        if (safeSearchEnabled && isGoogleDomain(domain)) {
            val spoofedResponse = if (domain.equals(GOOGLE_SAFE_SEARCH_HOST, ignoreCase = true)) {
                forwardDns(packet, length, output)
                return
            } else {
                Log.d("KioskyVpn", "SAFE_SEARCH: $domain qtype=$qtype -> CNAME $GOOGLE_SAFE_SEARCH_HOST")
                constructCnameDnsResponse(packet, GOOGLE_SAFE_SEARCH_HOST)
            }
            try {
                synchronized(output) { output.write(spoofedResponse) }
            } catch (e: Exception) {
                Log.e("KioskyVpn", "Failed to write safe search response", e)
            }
            return
        }

        if (youtubeSafeSearchEnabled && isYouTubeDomain(domain)) {
            // Use DNS aliasing (CNAME) to restrict.youtube.com so we don't rely on hardcoded VIP IPs.
            // IMPORTANT: Do not rewrite restrict.youtube.com itself, or we'll cause a CNAME loop.
            val spoofedResponse = if (domain.equals(YOUTUBE_RESTRICT_HOST, ignoreCase = true)) {
                forwardDns(packet, length, output)
                return
            } else {
                Log.d("KioskyVpn", "YOUTUBE_RESTRICT: $domain qtype=$qtype -> CNAME $YOUTUBE_RESTRICT_HOST")
                constructCnameDnsResponse(packet, YOUTUBE_RESTRICT_HOST)
            }
            try {
                synchronized(output) { output.write(spoofedResponse) }
            } catch (e: Exception) {
                Log.e("KioskyVpn", "Failed to write YouTube search response", e)
            }
            return
        }

        // 3. Normal Forwarding
        forwardDns(packet, length, output)
    }

    private fun forwardDns(packet: ByteBuffer, length: Int, output: FileOutputStream) {
        // Copy packet data to avoid race condition when the main loop reuses the buffer
        val packetData = ByteArray(length)
        System.arraycopy(packet.array(), 0, packetData, 0, length)

        scope.launch(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                val ihl = (packetData[0].toInt() and 0x0F) * 4
                val dnsPayloadOffset = ihl + 8
                
                if (packetData.size < dnsPayloadOffset) return@launch
                
                val dnsPayload = ByteArray(packetData.size - dnsPayloadOffset)
                System.arraycopy(packetData, dnsPayloadOffset, dnsPayload, 0, dnsPayload.size)

                socket = DatagramSocket()
                protect(socket)
                socket.soTimeout = 2000
                
                val realDns = InetAddress.getByName("8.8.8.8")
                socket.send(DatagramPacket(dnsPayload, dnsPayload.size, realDns, 53))

                val receiveBuffer = ByteArray(4096)
                val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                socket.receive(receivePacket)

                val fullResponse = constructDnsResponsePacket(ByteBuffer.wrap(packetData), receivePacket)
                synchronized(output) {
                    output.write(fullResponse)
                }
            } catch (e: Exception) {
                // Log.e("KioskyVpn", "DNS Forwarding error for a query", e)
            } finally {
                socket?.close()
            }
        }
    }

    private fun isDnsQuery(packet: ByteBuffer): Boolean {
        return try {
            val protocol = packet.get(9).toInt() and 0xFF
            val ihl = (packet.get(0).toInt() and 0x0F) * 4
            val destPort = ((packet.get(ihl + 2).toInt() and 0xFF) shl 8) or (packet.get(ihl + 3).toInt() and 0xFF)
            protocol == 17 && destPort == 53
        } catch (e: Exception) { false }
    }

    private fun getDnsDomain(packet: ByteBuffer): String? {
        return try {
            val ihl = (packet.get(0).toInt() and 0x0F) * 4
            var pos = ihl + 8 + 12
            val q = StringBuilder()
            while (pos < packet.limit()) {
                val len = packet.get(pos).toInt() and 0xFF
                if (len == 0) break
                if (q.isNotEmpty()) q.append(".")
                pos++
                for (i in 0 until len) {
                    if (pos + i < packet.limit()) {
                        q.append(packet.get(pos + i).toInt().toChar())
                    }
                }
                pos += len
            }
            q.toString()
        } catch (e: Exception) { null }
    }

    private fun getDnsQuestionType(packet: ByteBuffer): Int? {
        return try {
            val ihl = (packet.get(0).toInt() and 0x0F) * 4
            var pos = ihl + 8 + 12
            while (pos < packet.limit()) {
                val len = packet.get(pos).toInt() and 0xFF
                if (len == 0) {
                    pos++
                    break
                }
                pos += (len + 1)
            }
            if (pos + 1 >= packet.limit()) return null
            ((packet.get(pos).toInt() and 0xFF) shl 8) or (packet.get(pos + 1).toInt() and 0xFF)
        } catch (_: Exception) {
            null
        }
    }

    private fun isGoogleDomain(d: String): Boolean {
        val host = d.lowercase().trimEnd('.')
        if (host == GOOGLE_SAFE_SEARCH_HOST) return true
        if (host == "google.com" || host.endsWith(".google.com")) return true
        return host.startsWith("google.") || host.startsWith("www.google.")
    }
    private fun isYouTubeDomain(d: String): Boolean {
        val host = d.lowercase()
        return host.endsWith(".youtube.com") ||
            host == "youtube.com" ||
            host == "youtu.be" ||
            host.endsWith(".youtu.be") ||
            host == "youtubei.googleapis.com" ||
            host == "youtube.googleapis.com"
    }

    private fun isDoHDomain(d: String): Boolean {
        val host = d.lowercase().trimEnd('.')
        return DOH_BLOCKLIST.any { block ->
            host == block || host.endsWith(".$block")
        }
    }

    private fun matchesSafeSearchAdultDomain(domain: String): Boolean {
        val host = normalizeDomain(domain)
        if (host.isBlank()) return false
        if (isSafeSearchExemptDomain(host)) return false
        return SAFE_SEARCH_ADULT_DOMAIN_KEYWORDS.any { keyword ->
            keyword.isNotBlank() && host.contains(keyword)
        }
    }

    private fun isSafeSearchExemptDomain(domain: String): Boolean {
        return domain == GOOGLE_SAFE_SEARCH_HOST ||
            domain == "google.com" ||
            domain.endsWith(".google.com") ||
            domain.startsWith("google.") ||
            domain == YOUTUBE_RESTRICT_HOST ||
            domain == "youtube.com" ||
            domain.endsWith(".youtube.com") ||
            domain == "youtu.be" ||
            domain.endsWith(".youtu.be")
    }

    private fun constructEmptyDnsResponse(queryPacket: ByteBuffer): ByteArray {
        val ihl = (queryPacket.get(0).toInt() and 0x0F) * 4
        val dnsId = queryPacket.getShort(ihl + 8)
        val questionSection = extractDnsQuestionSection(queryPacket, ihl + 8)

        val payload = ByteBuffer.allocate(12 + questionSection.size)
        payload.putShort(dnsId)
        payload.putShort(0x8180.toShort()) // standard response, no error
        payload.putShort(1.toShort()) // QDCOUNT
        payload.putShort(0.toShort()) // ANCOUNT
        payload.putShort(0.toShort()) // NSCOUNT
        payload.putShort(0.toShort()) // ARCOUNT
        payload.put(questionSection)

        return constructDnsResponsePacket(queryPacket, DatagramPacket(payload.array(), payload.position()))
    }

    private fun encodeDnsName(name: String): ByteArray {
        val parts = name.trim('.').split('.').filter { it.isNotBlank() }
        val out = ArrayList<Byte>(name.length + 2)
        for (part in parts) {
            val bytes = part.toByteArray(Charsets.UTF_8)
            out.add(bytes.size.toByte())
            for (b in bytes) out.add(b)
        }
        out.add(0)
        val res = ByteArray(out.size)
        for (i in out.indices) res[i] = out[i]
        return res
    }

    private fun constructCnameDnsResponse(queryPacket: ByteBuffer, cnameTarget: String): ByteArray {
        val ihl = (queryPacket.get(0).toInt() and 0x0F) * 4
        val dnsId = queryPacket.getShort(ihl + 8)
        val questionSection = extractDnsQuestionSection(queryPacket, ihl + 8)
        val cnameBytes = encodeDnsName(cnameTarget)

        val answerSize = 2 + 2 + 2 + 4 + 2 + cnameBytes.size
        val payload = ByteBuffer.allocate(12 + questionSection.size + answerSize)
        payload.putShort(dnsId)
        payload.putShort(0x8180.toShort())
        payload.putShort(1.toShort()); payload.putShort(1.toShort())
        payload.putShort(0.toShort()); payload.putShort(0.toShort())
        payload.put(questionSection)

        payload.putShort(0xC00C.toShort()) // pointer to QNAME
        payload.putShort(5.toShort()) // CNAME
        payload.putShort(1.toShort()) // IN
        payload.putInt(300)
        payload.putShort(cnameBytes.size.toShort())
        payload.put(cnameBytes)

        return constructDnsResponsePacket(queryPacket, DatagramPacket(payload.array(), payload.position()))
    }

    private fun constructSpoofedDnsResponse(queryPacket: ByteBuffer, recordType: Int, ipBytes: ByteArray): ByteArray {
        val ihl = (queryPacket.get(0).toInt() and 0x0F) * 4
        val dnsId = queryPacket.getShort(ihl + 8)
        val questionSection = extractDnsQuestionSection(queryPacket, ihl + 8)
        
        val answerSize = 2 + 2 + 2 + 4 + 2 + ipBytes.size // name ptr + type + class + ttl + rdlen + rdata
        val payload = ByteBuffer.allocate(12 + questionSection.size + answerSize)
        payload.putShort(dnsId)
        payload.putShort(0x8180.toShort())
        payload.putShort(1.toShort()); payload.putShort(1.toShort())
        payload.putShort(0.toShort()); payload.putShort(0.toShort())
        payload.put(questionSection)
        payload.putShort(0xC00C.toShort())
        payload.putShort(recordType.toShort()); payload.putShort(1.toShort())
        payload.putInt(300); payload.putShort(ipBytes.size.toShort())
        payload.put(ipBytes)
        
        return constructDnsResponsePacket(queryPacket, DatagramPacket(payload.array(), payload.position()))
    }

    private fun extractDnsQuestionSection(packet: ByteBuffer, dnsOffset: Int): ByteArray {
        var pos = dnsOffset + 12
        while (pos < packet.limit()) {
            val len = packet.get(pos).toInt() and 0xFF
            if (len == 0) { pos++; break }
            pos += (len + 1)
        }
        pos += 4
        val length = pos - (dnsOffset + 12)
        if (length < 0) return ByteArray(0)
        val res = ByteArray(length)
        val originalPos = packet.position()
        packet.position(dnsOffset + 12)
        packet.get(res)
        packet.position(originalPos)
        return res
    }

    private fun constructDnsResponsePacket(query: ByteBuffer, dnsRes: DatagramPacket): ByteArray {
        val ihl = (query.get(0).toInt() and 0x0F) * 4
        val res = ByteArray(ihl + 8 + dnsRes.length)
        System.arraycopy(query.array(), 0, res, 0, ihl)
        
        // Swap IPs
        for (i in 0..3) {
            res[12 + i] = query.get(16 + i)
            res[16 + i] = query.get(12 + i)
        }
        
        val totalLen = res.size
        res[2] = (totalLen shr 8).toByte(); res[3] = (totalLen and 0xFF).toByte()
        res[10] = 0; res[11] = 0 // Checksum (computed below)
        
        // Swap Ports
        res[ihl] = query.get(ihl + 2); res[ihl + 1] = query.get(ihl + 3)
        res[ihl + 2] = query.get(ihl); res[ihl + 3] = query.get(ihl + 1)
        val udpLen = 8 + dnsRes.length
        res[ihl + 4] = (udpLen shr 8).toByte(); res[ihl + 5] = (udpLen and 0xFF).toByte()
        res[ihl + 6] = 0; res[ihl + 7] = 0 // UDP checksum (computed below)

        System.arraycopy(dnsRes.data, 0, res, ihl + 8, dnsRes.length)

        // UDP checksum (some stacks drop checksum=0 even on IPv4)
        val udpChecksum = udpChecksumIpv4(res, ihl, udpLen)
        res[ihl + 6] = (udpChecksum.toInt() shr 8).toByte()
        res[ihl + 7] = (udpChecksum.toInt() and 0xFF).toByte()

        // IPv4 header checksum (required)
        val checksum = ipv4HeaderChecksum(res, ihl)
        res[10] = (checksum.toInt() shr 8).toByte()
        res[11] = (checksum.toInt() and 0xFF).toByte()
        return res
    }

    private fun udpChecksumIpv4(packet: ByteArray, ipHeaderLen: Int, udpLen: Int): Short {
        var sum = 0

        // Pseudo-header: source IP (4) + dest IP (4) + zero/protocol (2) + UDP len (2)
        sum += ((packet[12].toInt() and 0xFF) shl 8) or (packet[13].toInt() and 0xFF)
        sum += ((packet[14].toInt() and 0xFF) shl 8) or (packet[15].toInt() and 0xFF)
        sum += ((packet[16].toInt() and 0xFF) shl 8) or (packet[17].toInt() and 0xFF)
        sum += ((packet[18].toInt() and 0xFF) shl 8) or (packet[19].toInt() and 0xFF)
        sum += 0x0011 // protocol UDP
        sum += udpLen and 0xFFFF

        // UDP header + payload
        var i = ipHeaderLen
        val end = ipHeaderLen + udpLen
        while (i < end) {
            val hi = packet[i].toInt() and 0xFF
            val lo = if (i + 1 < end) packet[i + 1].toInt() and 0xFF else 0
            sum += (hi shl 8) or lo
            i += 2
        }

        while ((sum ushr 16) != 0) sum = (sum and 0xFFFF) + (sum ushr 16)
        val checksum = (sum.inv() and 0xFFFF)
        return (if (checksum == 0) 0xFFFF else checksum).toShort()
    }

    private fun ipv4HeaderChecksum(packet: ByteArray, headerLen: Int): Short {
        var sum = 0
        var i = 0
        while (i < headerLen) {
            val hi = packet[i].toInt() and 0xFF
            val lo = packet[i + 1].toInt() and 0xFF
            sum += (hi shl 8) or lo
            i += 2
        }
        while ((sum ushr 16) != 0) sum = (sum and 0xFFFF) + (sum ushr 16)
        return (sum.inv() and 0xFFFF).toShort()
    }

    private fun stopVpn(removeForeground: Boolean = true) {
        syncJob?.cancel(); workerJob?.cancel()
        syncJob = null
        workerJob = null
        vpnInterface?.close(); vpnInterface = null
        setVpnRunning(false)
        if (removeForeground) {
            if (android.os.Build.VERSION.SDK_INT >= 24) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        }
    }

    private fun handleVpnFailure(message: String) {
        setVpnStatusMessage(message)
        mainHandler.post {
            try {
                Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
            } catch (_: Exception) {
            }
        }
    }

    private fun setVpnRunning(running: Boolean) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(VPN_RUNNING_PREF_KEY, running)
            .apply()
    }

    private fun setVpnStatusMessage(message: String?) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(VPN_STATUS_MESSAGE_PREF_KEY, message)
            .apply()
    }

    private fun buildForegroundNotification(): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Web Protection Active")
            .setContentText("kioskyApp is filtering websites and safe browsing requests.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    private fun startVpnInForeground() {
        val notification = buildForegroundNotification()
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            try {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
                )
                return
            } catch (e: Exception) {
                Log.w("KioskyVpn", "Manifest foreground start failed, retrying without explicit type", e)
            }
        }
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun ensureNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return

        val manager = getSystemService(NotificationManager::class.java) ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "kioskyApp VPN",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Ongoing web protection for kioskyApp"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

}
