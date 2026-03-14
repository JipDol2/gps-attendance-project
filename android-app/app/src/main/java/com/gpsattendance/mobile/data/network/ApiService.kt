package com.gpsattendance.mobile.data.network

import com.gpsattendance.mobile.data.model.LocationUpdateRequest
import com.gpsattendance.mobile.data.model.LocationUpdateResponse
import com.gpsattendance.mobile.data.model.LoginRequest
import com.gpsattendance.mobile.data.model.PageResponse
import com.gpsattendance.mobile.data.model.RegisterRequest
import com.gpsattendance.mobile.data.model.RefreshTokenRequest
import com.gpsattendance.mobile.data.model.TeamResponse
import com.gpsattendance.mobile.data.model.TokenResponse
import com.gpsattendance.mobile.data.model.WorkSessionResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
    @POST("api/v1/users/register")
    suspend fun register(@Body request: RegisterRequest): Response<Unit>

    @POST("api/v1/users/login")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

    @POST("api/v1/users/refresh")
    suspend fun refresh(@Body request: RefreshTokenRequest): Response<TokenResponse>
}

interface RefreshApi {
    @POST("api/v1/users/refresh")
    fun refreshBlocking(@Body request: RefreshTokenRequest): Call<TokenResponse>
}

interface AttendanceApi {
    @POST("api/v1/attendance/me/location")
    suspend fun updateMyLocation(@Body request: LocationUpdateRequest): Response<LocationUpdateResponse>

    @GET("api/v1/attendance/me/sessions")
    suspend fun mySessions(): Response<List<WorkSessionResponse>>

    @GET("api/v1/attendance/visible-sessions")
    suspend fun visibleSessions(): Response<PageResponse<WorkSessionResponse>>
}

interface TeamApi {
    @GET("api/v1/teams")
    suspend fun teams(): Response<List<TeamResponse>>
}
