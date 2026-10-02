package com.example.kioskyapp.apiServices

import com.example.kioskyapp.data.Result
import com.example.kioskyapp.models.ScreenTimeRuleRequest
import com.example.kioskyapp.models.ScreenTimeRuleResponse
import com.example.kioskyapp.models.ScreenTimeUsageResponse
import com.example.kioskyapp.models.LoginError
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class ScreenTimeApi(private val baseUrl: String = API_BASE_URL) {

    suspend fun saveScreenTimeRule(token: String, rule: ScreenTimeRuleRequest): Result<ScreenTimeRuleResponse> {
        return try {
            val response = ktorClient.post("$baseUrl/api/screen-time-rules") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(rule)
            }

            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                Result.Success(response.body<ScreenTimeRuleResponse>())
            } else {
                val error = try {
                    response.body<LoginError>().error
                } catch (e: Exception) {
                    "Failed to save screen time rule with status ${response.status}"
                }
                Result.Error(Exception(error))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getActiveRuleForChild(token: String, childId: String): Result<List<ScreenTimeRuleResponse>> {
        return try {
            val response = ktorClient.get("$baseUrl/api/screen-time-rules/child/$childId/active") {
                header("Authorization", "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                Result.Success(response.body<List<ScreenTimeRuleResponse>>())
            } else {
                Result.Error(Exception("Failed to fetch active rules"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun deactivateScreenTimeRule(token: String, ruleId: String): Result<ScreenTimeRuleResponse> {
        return try {
            val response = ktorClient.put("$baseUrl/api/screen-time-rules/$ruleId/deactivate") {
                header("Authorization", "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                Result.Success(response.body<ScreenTimeRuleResponse>())
            } else {
                Result.Error(Exception("Failed to deactivate screen time rule"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Backend returns an array of usage records for "today".
     * We sum them to compute total minutes used today.
     */
    suspend fun getTodayUsageMinutes(token: String, childId: String): Result<Int> {
        return try {
            val response = ktorClient.get("$baseUrl/api/screen-time-usage/today/$childId") {
                header("Authorization", "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                val list = response.body<List<ScreenTimeUsageResponse>>()
                Result.Success(list.sumOf { it.total_minutes })
            } else {
                Result.Error(Exception("Failed to fetch today's usage"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun upsertTodayUsageMinutes(
        token: String,
        childId: String,
        deviceId: String,
        usageDateIso: String,
        totalMinutes: Int
    ): Result<ScreenTimeUsageResponse> {
        return try {
            val response = ktorClient.post("$baseUrl/api/screen-time-usage/upsert") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(
                    mapOf(
                        "childId" to childId,
                        "deviceId" to deviceId,
                        "usageDate" to usageDateIso,
                        "totalMinutes" to totalMinutes
                    )
                )
            }

            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                Result.Success(response.body<ScreenTimeUsageResponse>())
            } else {
                val error = try {
                    response.body<LoginError>().error
                } catch (e: Exception) {
                    "Failed to upsert usage with status ${response.status}"
                }
                Result.Error(Exception(error))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getUsageHistory(token: String, childId: String): Result<List<ScreenTimeUsageResponse>> {
        return try {
            val response = ktorClient.get("$baseUrl/api/screen-time-usage/child/$childId") {
                header("Authorization", "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                Result.Success(response.body<List<ScreenTimeUsageResponse>>())
            } else {
                Result.Error(Exception("Failed to fetch screen time history"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
