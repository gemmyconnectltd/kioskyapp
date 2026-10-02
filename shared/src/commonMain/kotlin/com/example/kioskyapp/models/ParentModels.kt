package com.example.kioskyapp.models

import kotlinx.serialization.Serializable

// --- Parent Models ---
@Serializable
data class ParentUser(
    val id: String,
    val email: String,
    val full_name: String,
    val role: String = "PARENT"
)

@Serializable
data class AuthResponse(
    val message: String,
    val token: String,
    val user: ParentUser
)

@Serializable
data class LoginError(
    val error: String
)

// --- Child Models ---
@Serializable
data class ChildDTO(
    val full_name: String,
    val nickname: String? = null,
    val avatar: String? = null,
    val birth_date: String? = null
)

// Matches the FLAT response from Parent.controller.ts addChild function
@Serializable
data class AddChildResponse(
    val id: String,
    val full_name: String,
    val pincode: String? = null,
    val is_active: Boolean? = null,
    val nickname: String? = null,
    val avatar: String? = null
)

@Serializable
data class ChildActivationRequest(
    val pin_code: String,
    val device_id: String,
    val device_name: String,
    val os: String,
    val os_version: String,
    val push_token: String? = null
)

@Serializable
data class ChildAuthResponse(
    val message: String,
    val token: String,
    val child: ChildUser,
    val device: DeviceDTO
)

@Serializable
data class DeviceDTO(
    val id: String,
    val device_name: String,
    val os: String,
    val os_version: String? = null,
    val is_active: Boolean = true
)

@Serializable
data class ChildUser(
    val id: String,
    val full_name: String,
    val parent_id: String? = null,
    val role: String = "CHILD"
)
