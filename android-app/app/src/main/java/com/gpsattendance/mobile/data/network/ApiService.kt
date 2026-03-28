package com.gpsattendance.mobile.data.network

import com.gpsattendance.mobile.data.model.LocationUpdateRequest
import com.gpsattendance.mobile.data.model.LocationUpdateResponse
import com.gpsattendance.mobile.data.model.LoginRequest
import com.gpsattendance.mobile.data.model.PageResponse
import com.gpsattendance.mobile.data.model.PermissionMemberResponse
import com.gpsattendance.mobile.data.model.PermissionMemberAssignRequest
import com.gpsattendance.mobile.data.model.PermissionResponse
import com.gpsattendance.mobile.data.model.PermissionUpsertRequest
import com.gpsattendance.mobile.data.model.RegisterRequest
import com.gpsattendance.mobile.data.model.RefreshTokenRequest
import com.gpsattendance.mobile.data.model.DashboardSummaryResponse
import com.gpsattendance.mobile.data.model.TeamResponse
import com.gpsattendance.mobile.data.model.TeamAttendanceTodayResponse
import com.gpsattendance.mobile.data.model.TokenResponse
import com.gpsattendance.mobile.data.model.WorkSessionResponse
import com.gpsattendance.mobile.data.model.WorkPolicyResponse
import com.gpsattendance.mobile.data.model.WorkPolicyUpsertRequest
import com.gpsattendance.mobile.data.model.AttendanceHistoryItemResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

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

    @GET("api/v1/attendance/me/history")
    suspend fun myAttendanceHistory(
        @Query("month") month: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PageResponse<AttendanceHistoryItemResponse>>
}

interface DashboardApi {
    @GET("api/v1/dashboard/summary")
    suspend fun summary(): Response<DashboardSummaryResponse>
}

interface TeamApi {
    @GET("api/v1/teams")
    suspend fun teams(): Response<List<TeamResponse>>

    @GET("api/v1/teams/{teamId}/attendance/today")
    suspend fun teamAttendanceToday(
        @Path("teamId") teamId: Long
    ): Response<TeamAttendanceTodayResponse>
}

interface PolicyApi {
    @GET("api/v1/work-policies")
    suspend fun policies(): Response<List<WorkPolicyResponse>>

    @PUT("api/v1/work-policies/{policyId}")
    suspend fun updatePolicy(
        @Path("policyId") policyId: Long,
        @Body request: WorkPolicyUpsertRequest
    ): Response<WorkPolicyResponse>
}

interface PermissionApi {
    @GET("api/v1/permissions")
    suspend fun permissions(): Response<List<PermissionResponse>>

    @GET("api/v1/permissions/{permissionId}/members")
    suspend fun permissionMembers(
        @Path("permissionId") permissionId: Long
    ): Response<List<PermissionMemberResponse>>

    @POST("api/v1/permissions")
    suspend fun createPermission(@Body request: PermissionUpsertRequest): Response<PermissionResponse>

    @POST("api/v1/permissions/{permissionId}/members")
    suspend fun assignPermissionMember(
        @Path("permissionId") permissionId: Long,
        @Body request: PermissionMemberAssignRequest
    ): Response<Unit>

    @PUT("api/v1/permissions/{permissionId}")
    suspend fun updatePermission(
        @Path("permissionId") permissionId: Long,
        @Body request: PermissionUpsertRequest
    ): Response<PermissionResponse>

    @DELETE("api/v1/permissions/{permissionId}")
    suspend fun deletePermission(
        @Path("permissionId") permissionId: Long
    ): Response<Unit>

    @DELETE("api/v1/permissions/{permissionId}/members/{userId}")
    suspend fun unassignPermissionMember(
        @Path("permissionId") permissionId: Long,
        @Path("userId") userId: Long
    ): Response<Unit>
}
