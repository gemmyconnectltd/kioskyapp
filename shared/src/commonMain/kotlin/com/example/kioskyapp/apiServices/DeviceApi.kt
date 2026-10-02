package com.example.kioskyapp.apiServices

import com.example.kioskyapp.data.Result
import com.example.kioskyapp.models.DeviceDTO
import com.example.kioskyapp.models.LoginError
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode

class DeviceApi(private val baseUrl: String = API_BASE_URL) {

    suspend fun getDevicesByChildId(token: String, childId: String): Result<List<DeviceDTO>> {
        return try {
            val response = ktorClient.get("$baseUrl/api/devices/child/$childId") {
                header("Authorization", "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                Result.Success(response.body<List<DeviceDTO>>())
            } else {
                val error = try {
                    response.body<LoginError>().error
                } catch (_: Exception) {
                    "Failed to fetch devices with status ${response.status}"
                }
                Result.Error(Exception(error))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
