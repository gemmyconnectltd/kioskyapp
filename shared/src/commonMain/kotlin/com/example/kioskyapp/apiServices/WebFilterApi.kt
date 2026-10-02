package com.example.kioskyapp.apiServices

import com.example.kioskyapp.data.Result
import com.example.kioskyapp.models.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class WebFilterApi(private val baseUrl: String = API_BASE_URL) {

    suspend fun getContentFilters(token: String, childId: String): Result<List<ContentFilterDTO>> {
        return try {
            val response = ktorClient.get("$baseUrl/api/content-filters/child/$childId") {
                header("Authorization", "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                Result.Success(response.body<List<ContentFilterDTO>>())
            } else {
                Result.Error(Exception("Failed to fetch content filters: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun createContentFilter(
        token: String,
        request: CreateContentFilterRequest
    ): Result<ContentFilterDTO> {
        return try {
            val response = ktorClient.post("$baseUrl/api/content-filters") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                Result.Success(response.body<ContentFilterDTO>())
            } else {
                Result.Error(Exception("Failed to create content filter: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun updateContentFilter(
        token: String,
        filterId: String,
        request: UpdateContentFilterRequest
    ): Result<ContentFilterDTO> {
        return try {
            val response = ktorClient.put("$baseUrl/api/content-filters/$filterId") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (response.status == HttpStatusCode.OK) {
                Result.Success(response.body<ContentFilterDTO>())
            } else {
                Result.Error(Exception("Failed to update content filter: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun deleteContentFilter(token: String, filterId: String): Result<Boolean> {
        return try {
            val response = ktorClient.delete("$baseUrl/api/content-filters/$filterId") {
                header("Authorization", "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                Result.Success(true)
            } else {
                Result.Error(Exception("Failed to delete content filter: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
