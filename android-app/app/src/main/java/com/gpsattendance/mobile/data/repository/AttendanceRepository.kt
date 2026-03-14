package com.gpsattendance.mobile.data.repository

import com.gpsattendance.mobile.data.model.LocationUpdateRequest
import com.gpsattendance.mobile.data.model.LocationUpdateResponse
import com.gpsattendance.mobile.data.model.WorkSessionResponse
import com.gpsattendance.mobile.data.network.AttendanceApi
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepository @Inject constructor(
    private val attendanceApi: AttendanceApi
) {
    suspend fun updateMyLocation(latitude: Double, longitude: Double): Result<LocationUpdateResponse> = runCatching {
        val request = LocationUpdateRequest(latitude, longitude, LocalDateTime.now().withNano(0).toString())
        val response = attendanceApi.updateMyLocation(request)
        if (!response.isSuccessful) {
            val body = response.errorBody()?.string().orEmpty()
            if (response.code() == 401 || response.code() == 403) {
                throw AuthExpiredException("Session expired (${response.code()})")
            }
            throw IllegalStateException("?? ???? ?? (${response.code()}): $body")
        }
        response.body() ?: throw IllegalStateException("?? ???? ??? ?? ????")
    }

    suspend fun mySessions(): Result<List<WorkSessionResponse>> = runCatching {
        val response = attendanceApi.mySessions()
        if (!response.isSuccessful) {
            val body = response.errorBody()?.string().orEmpty()
            if (response.code() == 401 || response.code() == 403) {
                throw AuthExpiredException("Session expired (${response.code()})")
            }
            throw IllegalStateException("?? ?? ?? (${response.code()}): $body")
        }
        response.body().orEmpty()
    }

    suspend fun visibleSessions(): Result<List<WorkSessionResponse>> = runCatching {
        val response = attendanceApi.visibleSessions()
        if (!response.isSuccessful) {
            val body = response.errorBody()?.string().orEmpty()
            if (response.code() == 401 || response.code() == 403) {
                throw AuthExpiredException("Session expired (${response.code()})")
            }
            throw IllegalStateException("Visible sessions request failed (${response.code()}): $body")
        }
        response.body()?.content.orEmpty()
    }
}
