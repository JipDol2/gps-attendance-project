package com.gpsattendance.mobile.data.repository

import com.gpsattendance.mobile.data.model.TeamResponse
import com.gpsattendance.mobile.data.network.TeamApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeamRepository @Inject constructor(
    private val teamApi: TeamApi
) {
    suspend fun teams(): Result<List<TeamResponse>> = runCatching {
        val response = teamApi.teams()
        if (!response.isSuccessful) {
            val body = response.errorBody()?.string().orEmpty()
            if (response.code() == 401 || response.code() == 403) {
                throw AuthExpiredException("Session expired (${response.code()})")
            }
            throw IllegalStateException("Team list request failed (${response.code()}): $body")
        }
        response.body().orEmpty()
    }
}
