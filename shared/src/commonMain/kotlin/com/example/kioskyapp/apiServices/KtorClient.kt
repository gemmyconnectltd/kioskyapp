package com.example.kioskyapp.apiServices

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val DEFAULT_API_BASE_URL = "http://localhost:5001"

@Volatile
private var apiBaseUrlOverride: String? = null

val API_BASE_URL: String
    get() = apiBaseUrlOverride ?: DEFAULT_API_BASE_URL

fun configureApiBaseUrl(baseUrl: String?) {
    apiBaseUrlOverride = baseUrl
        ?.trim()
        ?.removeSuffix("/")
        ?.takeIf { it.isNotBlank() }
}

val ktorClient = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            useAlternativeNames = false
            coerceInputValues = true
        })
    }
    
    install(HttpTimeout) {
        requestTimeoutMillis = 30000 // 30 seconds
        connectTimeoutMillis = 15000 // 15 seconds
        socketTimeoutMillis = 15000 // 15 seconds
    }

    defaultRequest {
        header("Accept", "application/json")
        header("Content-Type", "application/json")
    }
}
