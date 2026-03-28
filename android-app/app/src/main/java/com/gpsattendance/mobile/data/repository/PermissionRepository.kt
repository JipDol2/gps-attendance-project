package com.gpsattendance.mobile.data.repository

import com.gpsattendance.mobile.data.model.PermissionMemberResponse
import com.gpsattendance.mobile.data.model.PermissionMemberAssignRequest
import com.gpsattendance.mobile.data.model.PermissionResponse
import com.gpsattendance.mobile.data.model.PermissionUpsertRequest
import com.gpsattendance.mobile.data.network.PermissionApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionRepository @Inject constructor(
    private val permissionApi: PermissionApi
) {
    suspend fun permissions(): Result<List<PermissionResponse>> = runCatching {
        val response = permissionApi.permissions()
        if (!response.isSuccessful) {
            val body = response.errorBody()?.string().orEmpty()
            if (response.code() == 401 || response.code() == 403) {
                throw AuthExpiredException("Session expired (${response.code()})")
            }
            throw IllegalStateException("Permissions request failed (${response.code()}): $body")
        }
        response.body().orEmpty()
    }

    suspend fun permissionMembers(permissionId: Long): Result<List<PermissionMemberResponse>> = runCatching {
        val response = permissionApi.permissionMembers(permissionId)
        if (!response.isSuccessful) {
            val body = response.errorBody()?.string().orEmpty()
            if (response.code() == 401 || response.code() == 403) {
                throw AuthExpiredException("Session expired (${response.code()})")
            }
            throw IllegalStateException("Permission members request failed (${response.code()}): $body")
        }
        response.body().orEmpty()
    }

    suspend fun createPermission(name: String, description: String?): Result<PermissionResponse> = runCatching {
        val response = permissionApi.createPermission(PermissionUpsertRequest(name = name, description = description))
        if (!response.isSuccessful) {
            val body = response.errorBody()?.string().orEmpty()
            if (response.code() == 401 || response.code() == 403) {
                throw AuthExpiredException("Session expired (${response.code()})")
            }
            throw IllegalStateException("Create permission request failed (${response.code()}): $body")
        }
        response.body() ?: throw IllegalStateException("Create permission response is empty")
    }

    suspend fun assignPermissionMember(permissionId: Long, userId: Long): Result<Unit> = runCatching {
        val response = permissionApi.assignPermissionMember(
            permissionId = permissionId,
            request = PermissionMemberAssignRequest(userId = userId)
        )
        if (!response.isSuccessful) {
            val body = response.errorBody()?.string().orEmpty()
            if (response.code() == 401 || response.code() == 403) {
                throw AuthExpiredException("Session expired (${response.code()})")
            }
            throw IllegalStateException("Assign permission member request failed (${response.code()}): $body")
        }
    }

    suspend fun unassignPermissionMember(permissionId: Long, userId: Long): Result<Unit> = runCatching {
        val response = permissionApi.unassignPermissionMember(permissionId = permissionId, userId = userId)
        if (!response.isSuccessful) {
            val body = response.errorBody()?.string().orEmpty()
            if (response.code() == 401 || response.code() == 403) {
                throw AuthExpiredException("Session expired (${response.code()})")
            }
            throw IllegalStateException("Unassign permission member request failed (${response.code()}): $body")
        }
    }
}
