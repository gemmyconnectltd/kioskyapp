package com.example.kioskyapp.vpn

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.kioskyapp.apiServices.AppManagementApi
import com.example.kioskyapp.apiServices.SearchLogApi
import com.example.kioskyapp.apiServices.SearchLogRequest
import com.example.kioskyapp.apiServices.WebFilterApi
import com.example.kioskyapp.data.Result
import com.example.kioskyapp.models.AppUsageSyncItem
import com.example.kioskyapp.models.ContentFilterDTO
import com.example.kioskyapp.models.FilterType
import com.example.kioskyapp.screentime.ScreenTimeLockActivity
import com.example.kioskyapp.screentime.ScreenTimeLockService
import com.example.kioskyapp.utils.KioskySettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class KioskyAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var settings: KioskySettings
    private val appManagementApi = AppManagementApi()
    private val searchLogApi = SearchLogApi()
    private val webFilterApi = WebFilterApi()
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var lastQuery: String = ""
    private var lastQueryTime: Long = 0
    private var syncJob: Job? = null
    private var enforcementJob: Job? = null

    @Volatile
    private var browserSafeSearchEnabled: Boolean = false

    @Volatile
    private var youtubeKeywordEnforcementEnabled: Boolean = false
    private val blockedKeywords = mutableSetOf<String>()
    private val blockedWebsiteDomains = mutableSetOf<String>()
    private val blockedPackages = mutableSetOf<String>()
    private val appDailyLimits = mutableMapOf<String, Int>()
    private val appNamesByPackage = mutableMapOf<String, String>()
    private var currentForegroundPackage: String? = null
    private var currentPackageSessionStartedAt: Long = 0L
    private var lastBlockedWebsite: String = ""
    private var lastBlockedWebsiteTime: Long = 0L

    private val FILTER_SYNC_INTERVAL_MS = 10_000L
    private val BLOCKED_WEBSITE_CACHE_KEY = "blocked_website_domains_cache"

    private val defaultAdultKeywords = setOf(
        "porn", "porno", "pornhub", "xnxx", "xvideos", "xvideo", "sex", "xxx", "nude", "nudes", "naked",
        "blowjob", "handjob", "anal", "hentai", "bdsm", "erotic", "escort",
        "milf", "strip", "onlyfans", "boobs", "tits", "pussy", "dick", "cock"
    )
    private val defaultAdultWebsiteKeywords = setOf(
        "pornhub", "xnxx", "xvideos", "xvideo", "youporn", "redtube",
        "xhamster", "spankbang", "tube8", "tnaflix", "beeg", "brazzers", "onlyfans"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        settings = KioskySettings(applicationContext)
        Log.d("KioskyAccessibility", "Service Connected")
        loadCachedBlockedWebsiteDomains()

        syncJob?.cancel()
        syncJob = scope.launch {
            while (isActive) {
                syncEnforcementSettings()
                syncTrackedAppUsage()
                delay(FILTER_SYNC_INTERVAL_MS)
            }
        }

        enforcementJob?.cancel()
        enforcementJob = scope.launch {
            while (isActive) {
                enforceCurrentAppPolicy()
                delay(2_000)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString().orEmpty()
        handleForegroundPackage(packageName)

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {

            val nodeInfo = event.source ?: rootInActiveWindow
            if (nodeInfo != null) {
                findAndLogSearch(nodeInfo, packageName)
            }
        }

        maybeBlockVisibleWebsite(packageName)
    }

    private fun findAndLogSearch(node: AccessibilityNodeInfo, packageName: String) {
        val isBrowser = isBrowserPackage(packageName)
        val isYouTube = isYouTubePackage(packageName)

        if (!isBrowser && !isYouTube) return

        // 1. Try to get text from the node if it looks like a search bar
        val text = node.text?.toString() ?: ""
        
        // 2. Logic to detect if this is a "completed" search
        // For simplicity, we check if the user is typing in a field that has 'search' in its ID or description
        val viewId = node.viewIdResourceName ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        
        if (viewId.contains("search", ignoreCase = true) || 
            contentDesc.contains("search", ignoreCase = true) ||
            isYouTube && viewId.contains("edit_text", ignoreCase = true)) {
            
            if (text.length > 3 && text != lastQuery) {
                // Debounce to avoid logging every character
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastQueryTime > 3000) { // 3 seconds gap
                    if (isBrowser && browserSafeSearchEnabled && containsBlockedKeyword(text)) {
                        Log.i("KioskyAccessibility", "Blocked browser search query: $text")
                        blockCurrentApp("Blocked sensitive search")
                        logSearch(text, "google")
                        lastQuery = text
                        lastQueryTime = currentTime
                        return
                    }
                    if (isYouTube && youtubeKeywordEnforcementEnabled && containsBlockedKeyword(text)) {
                        Log.i("KioskyAccessibility", "Blocked YouTube search query: $text")
                        blockCurrentApp("Blocked sensitive search")
                        // Optionally log the blocked query as a normal YouTube search if logging is enabled.
                        logSearch(text, "youtube")
                        lastQuery = text
                        lastQueryTime = currentTime
                        return
                    }
                    logSearch(text, if (isYouTube) "youtube" else "google")
                    lastQuery = text
                    lastQueryTime = currentTime
                }
            }
        }

        // Recursively check children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                findAndLogSearch(child, packageName)
            }
        }
    }

    private fun logSearch(query: String, engine: String) {
        val token = settings.getString("child_token")
        val childId = settings.getString("child_id")
        val deviceId = settings.getString("device_id")

        if (token.isNullOrBlank() || childId.isNullOrBlank() || deviceId.isNullOrBlank()) return
        
        // Only log if search logging is enabled in settings
        val loggingEnabled = settings.getString("search_logging_enabled") == "true"
        if (!loggingEnabled) return

        scope.launch {
            val request = SearchLogRequest(
                child_id = childId,
                device_id = deviceId,
                query = query,
                engine = engine
            )
            val result = searchLogApi.createSearchLog(token, request)
            if (result is Result.Success) {
                Log.i("KioskyAccessibility", "Logged search: $query on $engine")
            }
        }
    }

    private suspend fun syncEnforcementSettings() {
        val token = settings.getString("child_token")
        val childId = settings.getString("child_id")
        if (token.isNullOrBlank() || childId.isNullOrBlank()) return

        val result = webFilterApi.getContentFilters(token, childId)
        if (result is Result.Success) {
            val categories = result.data.filter { it.filter_type == FilterType.CATEGORY }
            browserSafeSearchEnabled = categories.any { it.value == "SAFE_SEARCH" && it.is_blocked }
            youtubeKeywordEnforcementEnabled = categories.any { it.value == "YOUTUBE_SAFE_SEARCH" && it.is_blocked }

            val newBlockedDomains = result.data
                .filter {
                    (it.filter_type == FilterType.WEBSITE || it.filter_type == FilterType.KEYWORD) &&
                        it.is_blocked
                }
                .map { normalizeWebsiteRule(it.value) }
                .filter { it.isNotBlank() }
                .toSet()

            synchronized(blockedWebsiteDomains) {
                blockedWebsiteDomains.clear()
                blockedWebsiteDomains.addAll(newBlockedDomains)
            }
            settings.putString(BLOCKED_WEBSITE_CACHE_KEY, newBlockedDomains.sorted().joinToString(","))

            val newBlocked = mutableSetOf<String>()
            newBlocked.addAll(defaultAdultKeywords)
            result.data
                .filter { it.filter_type == FilterType.KEYWORD && it.is_blocked }
                .mapNotNull { it.value.takeIf { v -> v.isNotBlank() }?.lowercase() }
                .forEach { newBlocked.add(it) }

            synchronized(blockedKeywords) {
                blockedKeywords.clear()
                blockedKeywords.addAll(newBlocked)
            }

            Log.d(
                "KioskyAccessibility",
                "Sync OK: safeSearch=$browserSafeSearchEnabled youtubeEnforce=$youtubeKeywordEnforcementEnabled keywords=${blockedKeywords.size} domains=${newBlockedDomains.size}"
            )
        }

        val appResult = appManagementApi.getCurrentChildDeviceApps(token)
        if (appResult is Result.Success) {
            val newBlockedPackages = mutableSetOf<String>()
            val newDailyLimits = mutableMapOf<String, Int>()
            val newAppNames = mutableMapOf<String, String>()

            appResult.data.forEach { app ->
                val normalizedPackage = app.package_name.lowercase()
                newAppNames[normalizedPackage] = app.app_name
                if (app.is_blocked) {
                    newBlockedPackages.add(normalizedPackage)
                }
                val limit = app.daily_limit_min ?: 0
                if (limit > 0) {
                    newDailyLimits[normalizedPackage] = limit
                }
            }

            synchronized(blockedPackages) {
                blockedPackages.clear()
                blockedPackages.addAll(newBlockedPackages)
            }

            synchronized(appDailyLimits) {
                appDailyLimits.clear()
                appDailyLimits.putAll(newDailyLimits)
            }

            synchronized(appNamesByPackage) {
                appNamesByPackage.clear()
                appNamesByPackage.putAll(newAppNames)
            }
        }
    }

    private fun containsBlockedKeyword(query: String): Boolean {
        val q = query.lowercase()
        return synchronized(blockedKeywords) {
            blockedKeywords.any { kw -> kw.isNotBlank() && q.contains(kw) }
        }
    }

    private fun loadCachedBlockedWebsiteDomains() {
        val cached = settings.getString(BLOCKED_WEBSITE_CACHE_KEY, "")
            ?.split(",")
            ?.map { normalizeWebsiteRule(it) }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            .orEmpty()

        if (cached.isEmpty()) return

        synchronized(blockedWebsiteDomains) {
            if (blockedWebsiteDomains.isEmpty()) {
                blockedWebsiteDomains.addAll(cached)
            }
        }
    }

    private fun normalizeWebsiteRule(value: String): String {
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

    private fun isKeywordWebsiteRule(rule: String): Boolean {
        return rule.isNotBlank() && !rule.contains('.')
    }

    private fun matchesBlockedWebsiteRule(candidate: String, rule: String): Boolean {
        val normalizedCandidate = normalizeWebsiteRule(candidate)
        val normalizedRule = normalizeWebsiteRule(rule)
        if (normalizedCandidate.isBlank() || normalizedRule.isBlank()) return false

        return if (isKeywordWebsiteRule(normalizedRule)) {
            normalizedCandidate.contains(normalizedRule)
        } else {
            normalizedCandidate == normalizedRule || normalizedCandidate.endsWith(".$normalizedRule")
        }
    }

    private fun isBrowserPackage(packageName: String): Boolean {
        val normalized = packageName.lowercase()
        return normalized.contains("chrome") ||
            normalized.contains("browser") ||
            normalized.contains("firefox") ||
            normalized.contains("opera") ||
            normalized.contains("brave") ||
            normalized.contains("edge") ||
            normalized.contains("duckduckgo") ||
            normalized.contains("kiwi") ||
            normalized.contains("vivaldi") ||
            normalized.contains("sbrowser")
    }

    private fun maybeBlockVisibleWebsite(packageName: String) {
        if (!isBrowserPackage(packageName)) return

        val root = rootInActiveWindow ?: return
        val candidates = linkedSetOf<String>()
        collectWebsiteCandidates(root, candidates)
        if (candidates.isEmpty()) return

        for (candidate in candidates) {
            val normalizedCandidate = normalizeWebsiteRule(candidate)
            if (isSafeSearchExemptWebsite(normalizedCandidate)) continue

            val matched = synchronized(blockedWebsiteDomains) {
                blockedWebsiteDomains.firstOrNull { rule -> matchesBlockedWebsiteRule(normalizedCandidate, rule) }
            } ?: synchronized(blockedKeywords) {
                if (browserSafeSearchEnabled) {
                    defaultAdultWebsiteKeywords.firstOrNull { keyword ->
                        keyword.isNotBlank() && normalizedCandidate.contains(keyword)
                    }
                } else {
                    null
                }
            } ?: continue

            val now = System.currentTimeMillis()
            if (matched == lastBlockedWebsite && now - lastBlockedWebsiteTime < 1500L) {
                return
            }

            lastBlockedWebsite = matched
            lastBlockedWebsiteTime = now
            persistCurrentSession()
            blockCurrentWebsite(packageName, normalizedCandidate)
            return
        }
    }

    private fun isSafeSearchExemptWebsite(domain: String): Boolean {
        if (domain.isBlank()) return true
        return domain == "google.com" ||
            domain.endsWith(".google.com") ||
            domain.startsWith("google.") ||
            domain == "forcesafesearch.google.com" ||
            domain == "youtube.com" ||
            domain.endsWith(".youtube.com") ||
            domain == "youtu.be" ||
            domain.endsWith(".youtu.be") ||
            domain == "restrict.youtube.com"
    }

    private fun collectWebsiteCandidates(node: AccessibilityNodeInfo, candidates: MutableSet<String>) {
        if (candidates.size >= 12) return

        addWebsiteCandidate(
            candidates = candidates,
            rawValue = node.text?.toString(),
            viewId = node.viewIdResourceName,
            contentDescription = node.contentDescription?.toString()
        )
        addWebsiteCandidate(
            candidates = candidates,
            rawValue = node.contentDescription?.toString(),
            viewId = node.viewIdResourceName,
            contentDescription = node.contentDescription?.toString()
        )

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectWebsiteCandidates(child, candidates)
        }
    }

    private fun addWebsiteCandidate(
        candidates: MutableSet<String>,
        rawValue: String?,
        viewId: String?,
        contentDescription: String?
    ) {
        val trimmed = rawValue?.trim()?.takeIf { it.isNotBlank() } ?: return
        if (trimmed.length > 255 || trimmed.contains('\n')) return

        val context = listOf(viewId.orEmpty(), contentDescription.orEmpty())
            .joinToString(" ")
            .lowercase()
        val looksLikeAddressField = context.contains("url") ||
            context.contains("address") ||
            context.contains("location") ||
            context.contains("omnibox") ||
            context.contains("search_box")
        val isSingleToken = trimmed.none { it.isWhitespace() }
        val looksLikeWebsite = trimmed.startsWith("http", ignoreCase = true) ||
            trimmed.startsWith("www.", ignoreCase = true) ||
            (isSingleToken && (trimmed.contains('.') || looksLikeAddressField))

        if (!looksLikeWebsite) return

        val normalized = normalizeWebsiteRule(trimmed)
        if (normalized.isNotBlank()) {
            candidates.add(normalized)
        }

        if (!isSingleToken) {
            addWebsiteTokensFromText(trimmed, candidates)
        }
    }

    private fun addWebsiteTokensFromText(text: String, candidates: MutableSet<String>) {
        val tokenRegex = Regex("[A-Za-z0-9._:/?=&%-]+")
        tokenRegex.findAll(text).forEach { match ->
            if (candidates.size >= 12) return

            val token = match.value.trim().trim('.', ',', ';', ':', ')', '(', '"', '\'')
            if (token.isBlank()) return@forEach

            val looksLikeWebsiteToken = token.startsWith("http", ignoreCase = true) ||
                token.startsWith("www.", ignoreCase = true) ||
                token.contains('.')
            if (!looksLikeWebsiteToken) return@forEach

            val normalized = normalizeWebsiteRule(token)
            if (normalized.isNotBlank()) {
                candidates.add(normalized)
            }
        }
    }

    private fun isYouTubePackage(pkg: String): Boolean {
        val p = pkg.lowercase()
        return p == "com.google.android.youtube" ||
            p.startsWith("com.google.android.youtube") ||
            p.contains("youtube")
    }

    private fun isIgnoredForegroundPackage(packageName: String): Boolean {
        val packageLooksLikeSystemSurface =
            packageName.contains(".launcher") ||
                packageName.contains("launcher3") ||
                packageName.endsWith(".home") ||
                packageName.contains("systemui") ||
                packageName.contains("permissioncontroller") ||
                packageName.contains("aodservice") ||
                packageName.contains(".aod")

        val hasLaunchIntent = try {
            packageManager.getLaunchIntentForPackage(packageName) != null
        } catch (_: Exception) {
            true
        }

        return packageName == this.packageName ||
            packageName == "com.android.systemui" ||
            packageName == "com.google.android.permissioncontroller" ||
            packageName == "com.samsung.android.app.aodservice" ||
            packageName == "com.samsung.android.app.aodservice:main" ||
            packageName.startsWith("com.android.launcher") ||
            packageName.startsWith("com.google.android.apps.nexuslauncher") ||
            packageName.startsWith("com.sec.android.app.launcher") ||
            packageName.startsWith("com.miui.home") ||
            packageName.startsWith("com.oppo.launcher") ||
            packageName.startsWith("com.coloros.launcher") ||
            packageName.startsWith("com.vivo.launcher") ||
            packageName.startsWith("com.transsion.XOSLauncher") ||
            packageName.startsWith("com.tecno.launcher") ||
            packageLooksLikeSystemSurface ||
            !hasLaunchIntent
    }

    private fun isScreenTimeLockActive(): Boolean {
        return settings.getString(ScreenTimeLockService.LOCK_SCREEN_ACTIVE_KEY, "false") == "true"
    }

    private fun isAllowedDuringScreenTimeLock(packageName: String): Boolean {
        val normalized = packageName.lowercase()
        if (normalized == this.packageName) return true

        return normalized == "com.android.systemui" ||
            normalized.contains("permissioncontroller") ||
            normalized.contains("dialer") ||
            normalized.contains("incallui") ||
            normalized == "com.android.phone" ||
            normalized.contains("contacts")
    }

    private fun enforceScreenTimeLock(packageName: String): Boolean {
        if (!isScreenTimeLockActive()) return false
        if (isAllowedDuringScreenTimeLock(packageName)) return false

        persistCurrentSession()
        mainHandler.post {
            try {
                Toast.makeText(
                    applicationContext,
                    "Only Phone and Contacts are available while screen time lock is active.",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (_: Exception) {
            }
        }

        try {
            startActivity(ScreenTimeLockActivity.createLaunchIntent(this))
        } catch (e: Exception) {
            Log.e("KioskyAccessibility", "Failed to reopen screen time lock", e)
        }
        return true
    }

    private fun handleForegroundPackage(packageName: String?) {
        val normalizedPackage = packageName?.lowercase()?.trim().orEmpty()
        if (normalizedPackage.isBlank()) return

        if (enforceScreenTimeLock(normalizedPackage)) {
            currentForegroundPackage = null
            currentPackageSessionStartedAt = 0L
            return
        }

        if (isIgnoredForegroundPackage(normalizedPackage)) {
            if (currentForegroundPackage != null) {
                persistCurrentSession()
                currentForegroundPackage = null
                currentPackageSessionStartedAt = 0L
            }
            return
        }

        if (currentForegroundPackage == normalizedPackage) return

        persistCurrentSession()
        currentForegroundPackage = normalizedPackage
        currentPackageSessionStartedAt = System.currentTimeMillis()

        if (isBlockedPackage(normalizedPackage)) {
            blockCurrentApp(buildAppMessage(normalizedPackage, "is blocked by your parent"))
            return
        }

        if (hasExceededDailyLimit(normalizedPackage)) {
            persistCurrentSession()
            blockCurrentApp(buildAppMessage(normalizedPackage, "has reached its daily limit"))
        }
    }

    private fun enforceCurrentAppPolicy() {
        val activePackage = currentForegroundPackage ?: return

        if (enforceScreenTimeLock(activePackage)) {
            currentForegroundPackage = null
            currentPackageSessionStartedAt = 0L
            return
        }

        if (isBlockedPackage(activePackage)) {
            persistCurrentSession()
            blockCurrentApp(buildAppMessage(activePackage, "is blocked by your parent"))
            return
        }

        if (hasExceededDailyLimit(activePackage)) {
            persistCurrentSession()
            blockCurrentApp(buildAppMessage(activePackage, "has reached its daily limit"))
        }
    }

    private fun isBlockedPackage(packageName: String): Boolean {
        return synchronized(blockedPackages) {
            blockedPackages.contains(packageName)
        }
    }

    private fun hasExceededDailyLimit(packageName: String): Boolean {
        val limitMinutes = synchronized(appDailyLimits) {
            appDailyLimits[packageName]
        } ?: return false

        val usedMs = getPersistedUsageMs(packageName) + getActiveSessionElapsedMs(packageName)
        return usedMs >= limitMinutes * 60_000L
    }

    private fun persistCurrentSession() {
        val childId = settings.getString("child_id") ?: return
        val deviceId = settings.getString("device_id") ?: return
        val activePackage = currentForegroundPackage ?: return

        val startedAt = currentPackageSessionStartedAt
        if (startedAt <= 0L) return

        val elapsed = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L)
        if (elapsed <= 0L) return

        val key = appUsageKey(childId, deviceId, activePackage)
        val stored = settings.getString(key, "0")?.toLongOrNull() ?: 0L
        settings.putString(key, (stored + elapsed).toString())
        rememberTrackedPackage(childId, deviceId, activePackage)
        currentPackageSessionStartedAt = System.currentTimeMillis()
    }

    private fun getPersistedUsageMs(packageName: String): Long {
        val childId = settings.getString("child_id") ?: return 0L
        val deviceId = settings.getString("device_id") ?: return 0L
        val key = appUsageKey(childId, deviceId, packageName)
        return settings.getString(key, "0")?.toLongOrNull() ?: 0L
    }

    private fun getActiveSessionElapsedMs(packageName: String): Long {
        if (currentForegroundPackage != packageName) return 0L
        if (currentPackageSessionStartedAt <= 0L) return 0L
        return (System.currentTimeMillis() - currentPackageSessionStartedAt).coerceAtLeast(0L)
    }

    private fun appUsageKey(childId: String, deviceId: String, packageName: String): String {
        val dayStamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return "app_usage_${childId}_${deviceId}_${packageName}_$dayStamp"
    }

    private fun appUsageRegistryKey(childId: String, deviceId: String): String {
        val dayStamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return "app_usage_registry_${childId}_${deviceId}_$dayStamp"
    }

    private fun rememberTrackedPackage(childId: String, deviceId: String, packageName: String) {
        val key = appUsageRegistryKey(childId, deviceId)
        val current = settings.getString(key, "")
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.toMutableSet()
            ?: mutableSetOf()

        if (current.add(packageName)) {
            settings.putString(key, current.sorted().joinToString(","))
        }
    }

    private suspend fun syncTrackedAppUsage() {
        val token = settings.getString("child_token") ?: return
        val childId = settings.getString("child_id") ?: return
        val deviceId = settings.getString("device_id") ?: return

        persistCurrentSession()

        val registryKey = appUsageRegistryKey(childId, deviceId)
        val trackedPackages = settings.getString(registryKey, "")
            ?.split(",")
            ?.map { it.trim().lowercase() }
            ?.filter { it.isNotBlank() && !isIgnoredForegroundPackage(it) }
            ?.distinct()
            .orEmpty()

        if (trackedPackages.isEmpty()) return

        val usageDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val usages = trackedPackages.mapNotNull { packageName ->
            val totalMinutes = (getPersistedUsageMs(packageName) / 60_000L).toInt()
            if (totalMinutes <= 0) {
                null
            } else {
                val appName = synchronized(appNamesByPackage) {
                    appNamesByPackage[packageName]
                } ?: packageName

                AppUsageSyncItem(
                    package_name = packageName,
                    app_name = appName,
                    usage_date = usageDate,
                    total_minutes = totalMinutes
                )
            }
        }

        if (usages.isEmpty()) return

        when (val result = appManagementApi.syncAppUsage(token, usages)) {
            is Result.Success -> Log.d("KioskyAccessibility", "Synced app usage for ${usages.size} apps")
            is Result.Error -> Log.w("KioskyAccessibility", "Failed to sync app usage: ${result.exception.message}")
        }
    }

    private fun buildAppMessage(packageName: String, suffix: String): String {
        val appName = synchronized(appNamesByPackage) {
            appNamesByPackage[packageName]
        } ?: packageName
        return "$appName $suffix"
    }

    private fun blockCurrentWebsite(packageName: String, domain: String) {
        mainHandler.post {
            try {
                Toast.makeText(applicationContext, "$domain is blocked by your parent", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
            }
        }

        try {
            performGlobalAction(GLOBAL_ACTION_BACK)
            return
        } catch (_: Exception) {
        }

        blockCurrentApp("$domain is blocked by your parent")
    }

    private fun blockCurrentApp(message: String) {
        mainHandler.post {
            try {
                Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
            }
        }

        try {
            performGlobalAction(GLOBAL_ACTION_HOME)
        } catch (_: Exception) {
        }

        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("KioskyAccessibility", "Failed to redirect to home", e)
        }
    }

    override fun onInterrupt() {
        Log.d("KioskyAccessibility", "Service Interrupted")
    }

    override fun onDestroy() {
        persistCurrentSession()
        syncJob?.cancel()
        enforcementJob?.cancel()
        super.onDestroy()
    }
}
