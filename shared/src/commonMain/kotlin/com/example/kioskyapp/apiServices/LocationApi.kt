package com.example.kioskyapp.apiServices

import com.example.kioskyapp.data.Result
import com.example.kioskyapp.models.LocationLogRequest
import com.example.kioskyapp.models.LocationLogResponse
import com.example.kioskyapp.models.MostVisitedLocation
import com.example.kioskyapp.models.LoginError
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

class LocationApi(private val baseUrl: String = API_BASE_URL) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun sendLocationLog(token: String, log: LocationLogRequest): Result<LocationLogResponse> {
        return try {
            val url = "$baseUrl/api/location-logs"
            println("DEBUG: Posting location to: $url")
            val response = ktorClient.post(url) {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(log)
            }

            println("DEBUG: SendLocationLog Status: ${response.status}")

            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                Result.Success(response.body<LocationLogResponse>())
            } else {
                val error = try {
                    response.body<LoginError>().error
                } catch (e: Exception) {
                    "Failed to send location with status ${response.status}"
                }
                println("DEBUG: SendLocationLog Server Error: $error")
                Result.Error(Exception(error))
            }
        } catch (e: Exception) {
            println("DEBUG: SendLocationLog Exception: ${e.message}")
            Result.Error(e)
        }
    }

    suspend fun getLatestLocation(token: String, childId: String): Result<LocationLogResponse?> {
        return try {
            val url = "$baseUrl/api/location-logs/child/$childId/latest"
            println("DEBUG: Fetching location from: $url")
            val response = ktorClient.get(url) {
                header("Authorization", "Bearer $token")
            }

            println("DEBUG: GetLatestLocation Status: ${response.status}")

            if (response.status == HttpStatusCode.OK) {
                val bodyText = response.body<String>()
                println("DEBUG: GetLatestLocation Body: $bodyText")
                if (bodyText == "null" || bodyText.isBlank()) {
                    Result.Success(null)
                } else {
                    val location = json.decodeFromString<LocationLogResponse>(bodyText)
                    Result.Success(location)
                }
            } else {
                val error = try {
                    response.body<LoginError>().error
                } catch (e: Exception) {
                    "Failed to fetch location with status ${response.status}"
                }
                println("DEBUG: GetLatestLocation Error: $error")
                Result.Error(Exception(error))
            }
        } catch (e: Exception) {
            println("DEBUG: GetLatestLocation Exception: ${e.message}")
            Result.Error(e)
        }
    }

    suspend fun getLocationHistory(token: String, childId: String): Result<List<LocationLogResponse>> {
        return try {
            // Ask backend for a small number; UI only shows the top few anyway
            val response = ktorClient.get("$baseUrl/api/location-logs/child/$childId?limit=5") {
                header("Authorization", "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                Result.Success(response.body<List<LocationLogResponse>>())
            } else {
                val error = try {
                    response.body<LoginError>().error
                } catch (e: Exception) {
                    "Failed to fetch history with status ${response.status}"
                }
                Result.Error(Exception(error))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getMostVisitedLocations(
        token: String,
        childId: String,
        limit: Int = 5
    ): Result<List<MostVisitedLocation>> {
        return try {
            val url = "$baseUrl/api/location-logs/child/$childId/most-visited?limit=$limit"
            println("DEBUG: Fetching most visited from: $url")
            val response = ktorClient.get(url) {
                header("Authorization", "Bearer $token")
            }

            println("DEBUG: GetMostVisited Status: ${response.status}")

            if (response.status == HttpStatusCode.OK) {
                val bodyText = response.body<String>()
                println("DEBUG: GetMostVisited Body: $bodyText")
                if (bodyText == "null" || bodyText.isBlank() || bodyText == "[]") {
                    Result.Success(emptyList())
                } else {
                    val locations = json.decodeFromString<List<MostVisitedLocation>>(bodyText)
                    Result.Success(locations)
                }
            } else {
                val error = try {
                    response.body<LoginError>().error
                } catch (e: Exception) {
                    "Failed to fetch most visited with status ${response.status}"
                }
                println("DEBUG: GetMostVisited Error: $error")
                Result.Error(Exception(error))
            }
        } catch (e: Exception) {
            println("DEBUG: GetMostVisited Exception: ${e.message}")
            Result.Error(e)
        }
    }
}
