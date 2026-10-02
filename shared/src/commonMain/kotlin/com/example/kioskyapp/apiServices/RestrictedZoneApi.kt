package com.example.kioskyapp.apiServices

import com.example.kioskyapp.data.Result
import com.example.kioskyapp.models.CreateRestrictedZoneRequest
import com.example.kioskyapp.models.LoginError
import com.example.kioskyapp.models.RestrictedZoneDTO
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

class RestrictedZoneApi(private val baseUrl: String = API_BASE_URL) {

    suspend fun getRestrictedZones(token: String, childId: String): Result<List<RestrictedZoneDTO>> {
        return try {
            val response = ktorClient.get("$baseUrl/api/restricted-zones/child/$childId") {
                header("Authorization", "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                Result.Success(response.body<List<RestrictedZoneDTO>>())
            } else {
                val error = try {
                    response.body<LoginError>().error
                } catch (_: Exception) {
                    "Failed to fetch restricted zones with status ${response.status}"
                }
                Result.Error(Exception(error))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun createRestrictedZone(
        token: String,
        request: CreateRestrictedZoneRequest
    ): Result<RestrictedZoneDTO> {
        return try {
            val response = ktorClient.post("$baseUrl/api/restricted-zones") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                Result.Success(response.body<RestrictedZoneDTO>())
            } else {
                val error = try {
                    response.body<LoginError>().error
                } catch (_: Exception) {
                    "Failed to create restricted zone with status ${response.status}"
                }
                Result.Error(Exception(error))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
