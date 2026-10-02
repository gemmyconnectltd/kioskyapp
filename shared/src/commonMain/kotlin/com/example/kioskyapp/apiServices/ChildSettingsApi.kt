package com.example.kioskyapp.apiServices

import com.example.kioskyapp.data.Result
import com.example.kioskyapp.models.LoginError
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class AdvancedSettingsRequest(
    val safe_search_enabled: Boolean? = null,
    val youtube_safe_search_enabled: Boolean? = null,
    val search_logging_enabled: Boolean? = null,
    val youtube_monitoring_enabled: Boolean? = null
)

class ChildSettingsApi(private val baseUrl: String = API_BASE_URL) {

    suspend fun updateAdvancedSettings(
        token: String,
        childId: String,
        settings: AdvancedSettingsRequest
    ): Result<Boolean> {
        return try {
            val url = "$baseUrl/api/children/$childId"
            val response = ktorClient.put(url) {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(settings)
            }

            if (response.status == HttpStatusCode.OK) {
                Result.Success(true)
            } else {
                val error = try {
                    response.body<LoginError>().error
                } catch (e: Exception) {
                    "Failed to update settings with status ${response.status}"
                }
                Result.Error(Exception(error))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
