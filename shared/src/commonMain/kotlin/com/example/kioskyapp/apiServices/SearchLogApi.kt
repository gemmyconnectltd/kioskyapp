package com.example.kioskyapp.apiServices

import com.example.kioskyapp.data.Result
import com.example.kioskyapp.models.LoginError
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class SearchLogRequest(
    val child_id: String,
    val device_id: String,
    val query: String,
    val engine: String = "google"
)

@Serializable
data class SearchLogResponse(
    val id: String,
    val child_id: String,
    val device_id: String,
    val query: String,
    val engine: String,
    val recorded_at: String
)

class SearchLogApi(private val baseUrl: String = API_BASE_URL) {

    suspend fun createSearchLog(
        token: String,
        log: SearchLogRequest
    ): Result<SearchLogResponse> {
        return try {
            val url = "$baseUrl/api/search-logs"
            val response = ktorClient.post(url) {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(log)
            }

            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                Result.Success(response.body<SearchLogResponse>())
            } else {
                val error = try {
                    response.body<LoginError>().error
                } catch (e: Exception) {
                    "Failed to create search log with status ${response.status}"
                }
                Result.Error(Exception(error))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getSearchLogsByChild(
        token: String,
        childId: String
    ): Result<List<SearchLogResponse>> {
        return try {
            val url = "$baseUrl/api/search-logs/child/$childId"
            val response = ktorClient.get(url) {
                header("Authorization", "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                Result.Success(response.body<List<SearchLogResponse>>())
            } else {
                val error = try {
                    response.body<LoginError>().error
                } catch (e: Exception) {
                    "Failed to fetch search logs with status ${response.status}"
                }
                Result.Error(Exception(error))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
