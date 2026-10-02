package com.example.kioskyapp.apiServices

import com.example.kioskyapp.data.Result
import com.example.kioskyapp.models.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class AuthApi(private val baseUrl: String = API_BASE_URL) {

    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            val response = ktorClient.post("$baseUrl/api/parents/login") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("email" to email, "password" to password))
            }

            if (response.status == HttpStatusCode.OK) {
                Result.Success(response.body<AuthResponse>())
            } else {
                val error = try {
                    response.body<LoginError>().error
                } catch (e: Exception) {
                    "Login failed with status ${response.status}"
                }
                Result.Error(Exception(error))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun register(fullName: String, email: String, password: String): Result<AuthResponse> {
        return try {
            val response = ktorClient.post("$baseUrl/api/parents/register") {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "full_name" to fullName,
                    "email" to email,
                    "password" to password
                ))
            }

            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                Result.Success(response.body<AuthResponse>())
            } else {
                val error = try {
                    response.body<LoginError>().error
                } catch (e: Exception) {
                    "Registration failed with status ${response.status}"
                }
                Result.Error(Exception(error))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun addChild(token: String, childData: ChildDTO): Result<AddChildResponse> {
        return try {
            val response = ktorClient.post("$baseUrl/api/parents/children") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(childData)
            }

            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                Result.Success(response.body<AddChildResponse>())
            } else {
                val error = try {
                    response.body<LoginError>().error
                } catch (e: Exception) {
                    "Failed to add child with status ${response.status}"
                }
                Result.Error(Exception(error))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getChildren(token: String, parentId: String): Result<List<AddChildResponse>> {
        return try {
            val response = ktorClient.get("$baseUrl/api/parents/$parentId/children") {
                header("Authorization", "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                Result.Success(response.body<List<AddChildResponse>>())
            } else {
                val error = try {
                    response.body<LoginError>().error
                } catch (e: Exception) {
                    "Failed to fetch children with status ${response.status}"
                }
                Result.Error(Exception(error))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun activateChild(activationData: ChildActivationRequest): Result<ChildAuthResponse> {
        return try {
            val response = ktorClient.post("$baseUrl/api/children/activate") {
                contentType(ContentType.Application.Json)
                setBody(activationData)
            }

            if (response.status == HttpStatusCode.OK) {
                Result.Success(response.body<ChildAuthResponse>())
            } else {
                val error = try {
                    response.body<LoginError>().error
                } catch (e: Exception) {
                    "Activation failed with status ${response.status}"
                }
                Result.Error(Exception(error))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
