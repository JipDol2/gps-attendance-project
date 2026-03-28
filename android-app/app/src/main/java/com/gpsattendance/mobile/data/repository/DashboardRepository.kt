package com.gpsattendance.mobile.data.repository

import com.gpsattendance.mobile.data.model.DashboardSummaryResponse
import com.gpsattendance.mobile.data.network.DashboardApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepository @Inject constructor(
    private val dashboardApi: DashboardApi
) {
    suspend fun summary(): Result<DashboardSummaryResponse> = runCatching {
        val response = dashboardApi.summary()
        if (!response.isSuccessful) {
            val body = response.errorBody()?.string().orEmpty()
            if (response.code() == 401 || response.code() == 403) {
                throw AuthExpiredException("Session expired (${response.code()})")
            }
            throw IllegalStateException("Dashboard summary request failed (${response.code()}): $body")
        }
        response.body() ?: throw IllegalStateException("Dashboard summary response is empty")
    }
}

