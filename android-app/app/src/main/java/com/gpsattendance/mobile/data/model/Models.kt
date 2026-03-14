package com.gpsattendance.mobile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val loginId: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val loginId: String,
    val email: String,
    val password: String,
    val name: String,
    val teamId: Long
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
data class UserResponse(
    val id: Long? = null,
    val loginId: String? = null,
    val email: String? = null,
    val name: String? = null,
    val roleLevel: String? = null,
    val hrAuthority: Boolean? = null,
    val active: Boolean? = null,
    val teamId: Long? = null,
    val workPolicyId: Long? = null,
    val createdAt: String? = null,
    val lastLoginAt: String? = null
)

@Serializable
data class TeamResponse(
    val id: Long,
    val name: String
)

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val user: UserResponse
)

@Serializable
data class LocationUpdateRequest(
    val latitude: Double,
    val longitude: Double,
    val observedAt: String? = null
)

@Serializable
data class LocationUpdateResponse(
    val state: String,
    val message: String,
    val sessionId: Long? = null,
    val distanceM: Double? = null
)

@Serializable
data class WorkSessionResponse(
    val sessionId: Long,
    val userId: Long,
    val userName: String,
    val status: String,
    val checkInAt: String? = null,
    val checkOutAt: String? = null,
    val outsideSince: String? = null,
    val lastLatitude: Double? = null,
    val lastLongitude: Double? = null
)

@Serializable
data class PageResponse<T>(
    val content: List<T> = emptyList()
)
