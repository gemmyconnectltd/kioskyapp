package com.example.kioskyapp.apiServices

import com.example.kioskyapp.data.Result
import com.example.kioskyapp.models.AppLimitRequest
import com.example.kioskyapp.models.AppRequestCreateRequest
import com.example.kioskyapp.models.AppRequestDto
import com.example.kioskyapp.models.AppUsageSummaryDto
import com.example.kioskyapp.models.AppUsageSyncItem
import com.example.kioskyapp.models.AppUsageSyncRequest
import com.example.kioskyapp.models.InstalledAppSyncItem
import com.example.kioskyapp.models.InstalledAppsSyncRequest
import com.example.kioskyapp.models.LoginError
import com.example.kioskyapp.models.ManagedAppDto
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode

class AppManagementApi(private val baseUrl: String = API_BASE_URL) {

    suspend fun getAppsByDevice(token: String, deviceId: String): Result<List<ManagedAppDto>> {
        return try {
            val response = ktorClient.get("$baseUrl/api/apps/device/$deviceId") {
                header("Authorization", "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                Result.Success(response.body<List<ManagedAppDto>>())
            } else {
                Result.Error(Exception(readError(response.status) { response.body<LoginError>().error }))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getCurrentChildDeviceApps(token: String): Result<List<ManagedAppDto>> {
        return try {
            val response = ktorClient.get("$baseUrl/api/apps/me/device-apps") {
                header("Authorization", "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                Result.Success(response.body<List<ManagedAppDto>>())
            } else {
                Result.Error(Exception(readError(response.status) { response.body<LoginError>().error }))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getCurrentChildRequests(token: String): Result<List<AppRequestDto>> {
        return try {
            val response = ktorClient.get("$baseUrl/api/app-requests/me") {
                header("Authorization", "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                Result.Success(response.body<List<AppRequestDto>>())
            } else {
                Result.Error(Exception(readError(response.status) { response.body<LoginError>().error }))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun syncInstalledApps(token: String, apps: List<InstalledAppSyncItem>): Result<List<ManagedAppDto>> {
        return try {
            val response = ktorClient.post("$baseUrl/api/apps/sync") {
                header("Authorization", "Bearer $token")
                setBody(InstalledAppsSyncRequest(apps))
            }

            if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) {
                Result.Success(response.body<List<ManagedAppDto>>())
            } else {
                Result.Error(Exception(readError(response.status) { response.body<LoginError>().error }))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun syncAppUsage(token: String, usages: List<AppUsageSyncItem>): Result<Boolean> {
        return try {
            val response = ktorClient.post("$baseUrl/api/apps/usage/upsert") {
                header("Authorization", "Bearer $token")
                setBody(AppUsageSyncRequest(usages))
            }

            if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) {
                Result.Success(true)
            } else {
                Result.Error(Exception(readError(response.status) { response.body<LoginError>().error }))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getAppUsageSummary(token: String, childId: String, period: String): Result<AppUsageSummaryDto> {
        return try {
            val response = ktorClient.get("$baseUrl/api/apps/usage/child/$childId?period=$period") {
                header("Authorization", "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                Result.Success(response.body<AppUsageSummaryDto>())
            } else {
                Result.Error(Exception(readError(response.status) { response.body<LoginError>().error }))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun blockApp(token: String, appId: String): Result<ManagedAppDto> {
        return try {
            val response = ktorClient.put("$baseUrl/api/apps/$appId/block") {
                header("Authorization", "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                Result.Success(response.body<ManagedAppDto>())
            } else {
                Result.Error(Exception(readError(response.status) { response.body<LoginError>().error }))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun unblockApp(token: String, appId: String): Result<ManagedAppDto> {
        return try {
            val response = ktorClient.put("$baseUrl/api/apps/$appId/unblock") {
                header("Authorization", "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                Result.Success(response.body<ManagedAppDto>())
            } else {
                Result.Error(Exception(readError(response.status) { response.body<LoginError>().error }))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun setDailyLimit(token: String, appId: String, minutes: Int): Result<ManagedAppDto> {
        return try {
            val response = ktorClient.put("$baseUrl/api/apps/$appId/daily-limit") {
                header("Authorization", "Bearer $token")
                setBody(AppLimitRequest(minutes))
            }

            if (response.status == HttpStatusCode.OK) {
                Result.Success(response.body<ManagedAppDto>())
            } else {
                Result.Error(Exception(readError(response.status) { response.body<LoginError>().error }))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun removeDailyLimit(token: String, appId: String): Result<ManagedAppDto> {
        return try {
            val response = ktorClient.put("$baseUrl/api/apps/$appId/remove-daily-limit") {
                header("Authorization", "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                Result.Success(response.body<ManagedAppDto>())
            } else {
                Result.Error(Exception(readError(response.status) { response.body<LoginError>().error }))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getPendingRequests(token: String, childId: String): Result<List<AppRequestDto>> {
        return try {
            val response = ktorClient.get("$baseUrl/api/app-requests/pending?childId=$childId") {
                header("Authorization", "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                Result.Success(response.body<List<AppRequestDto>>())
            } else {
                Result.Error(Exception(readError(response.status) { response.body<LoginError>().error }))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun approveRequest(token: String, requestId: String): Result<AppRequestDto> {
        return try {
            val response = ktorClient.put("$baseUrl/api/app-requests/$requestId/approve") {
                header("Authorization", "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                Result.Success(response.body<AppRequestDto>())
            } else {
                Result.Error(Exception(readError(response.status) { response.body<LoginError>().error }))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun denyRequest(token: String, requestId: String): Result<AppRequestDto> {
        return try {
            val response = ktorClient.put("$baseUrl/api/app-requests/$requestId/deny") {
                header("Authorization", "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                Result.Success(response.body<AppRequestDto>())
            } else {
                Result.Error(Exception(readError(response.status) { response.body<LoginError>().error }))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun createAppRequest(token: String, appName: String, packageName: String, reason: String): Result<AppRequestDto> {
        return try {
            val response = ktorClient.post("$baseUrl/api/app-requests") {
                header("Authorization", "Bearer $token")
                setBody(
                    AppRequestCreateRequest(
                        app_name = appName,
                        package_name = packageName,
                        reason = reason.ifBlank { null }
                    )
                )
            }

            if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) {
                Result.Success(response.body<AppRequestDto>())
            } else {
                Result.Error(Exception(readError(response.status) { response.body<LoginError>().error }))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private inline fun readError(status: HttpStatusCode, block: () -> String): String {
        return try {
            block()
        } catch (_: Exception) {
            "Request failed with status $status"
        }
    }
}
