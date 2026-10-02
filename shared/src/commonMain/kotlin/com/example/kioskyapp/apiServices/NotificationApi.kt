package com.example.kioskyapp.apiServices

import com.example.kioskyapp.data.Result
import com.example.kioskyapp.models.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class CreateNotificationRequest(
    val parent_id: String,
    val child_id: String?,
    val type: String,
    val title: String,
    val message: String
)

@Serializable
data class CreateEmergencyRequest(
    val child_id: String,
    val parent_id: String,
    val device_id: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val message: String? = null
)

@Serializable
data class NotificationResponse(
    val id: String,
    val parent_id: String,
    val child_id: String?,
    val type: String,
    val title: String,
    val message: String,
    val is_read: Boolean,
    val created_at: String
)

@Serializable
data class EmergencyResponse(
    val id: String,
    val child_id: String,
    val parent_id: String,
    val device_id: String?,
    val latitude: Double?,
    val longitude: Double?,
    val message: String?,
    val status: String,
    val created_at: String
)

class NotificationApi(private val baseUrl: String = API_BASE_URL) {

    suspend fun sendSOS(
        token: String, 
        parentId: String, 
        childId: String, 
        childName: String,
        latitude: Double? = null,
        longitude: Double? = null,
        deviceId: String? = null
    ): Result<EmergencyResponse> {
        return try {
            val response = ktorClient.post("$baseUrl/api/emergencies") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(CreateEmergencyRequest(
                    parent_id = parentId,
                    child_id = childId,
                    device_id = deviceId,
                    latitude = latitude,
                    longitude = longitude,
                    message = "🚨 SOS EMERGENCY! 🚨 $childName has pressed the SOS button! Please check their location immediately."
                ))
            }

            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                Result.Success(response.body<EmergencyResponse>())
            } else {
                Result.Error(Exception("Failed to send SOS: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun markEmergencyAsViewed(token: String, emergencyId: String): Result<Boolean> {
        return try {
            val response = ktorClient.put("$baseUrl/api/emergencies/$emergencyId/view") {
                header("Authorization", "Bearer $token")
            }
            if (response.status == HttpStatusCode.OK) {
                Result.Success(true)
            } else {
                Result.Error(Exception("Failed to mark emergency as viewed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getUnreadNotifications(token: String, parentId: String): Result<List<NotificationResponse>> {
        return try {
            val response = ktorClient.get("$baseUrl/api/notifications/parent/$parentId/unread") {
                header("Authorization", "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                Result.Success(response.body<List<NotificationResponse>>())
            } else {
                Result.Error(Exception("Failed to fetch notifications: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun markNotificationAsRead(token: String, notificationId: String): Result<Boolean> {
        return try {
            val response = ktorClient.put("$baseUrl/api/notifications/$notificationId/read") {
                header("Authorization", "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                Result.Success(true)
            } else {
                Result.Error(Exception("Failed to mark notification as read: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getUnviewedEmergencies(token: String, parentId: String): Result<List<EmergencyResponse>> {
        return try {
            val response = ktorClient.get("$baseUrl/api/emergencies/parent/$parentId/unviewed") {
                header("Authorization", "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                Result.Success(response.body<List<EmergencyResponse>>())
            } else {
                Result.Error(Exception("Failed to fetch emergencies: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
