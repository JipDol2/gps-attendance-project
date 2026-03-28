package com.gpsattendance.mobile.data.repository

import com.gpsattendance.mobile.data.model.WorkPolicyResponse
import com.gpsattendance.mobile.data.model.WorkPolicyUpsertRequest
import com.gpsattendance.mobile.data.network.PolicyApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PolicyRepository @Inject constructor(
    private val policyApi: PolicyApi
) {
    suspend fun policies(): Result<List<WorkPolicyResponse>> = runCatching {
        val response = policyApi.policies()
        if (!response.isSuccessful) {
            val body = response.errorBody()?.string().orEmpty()
            if (response.code() == 401 || response.code() == 403) {
                throw AuthExpiredException("Session expired (${response.code()})")
            }
            throw IllegalStateException("Work policies request failed (${response.code()}): $body")
        }
        response.body().orEmpty()
    }

    suspend fun updatePolicy(
        policyId: Long,
        request: WorkPolicyUpsertRequest
    ): Result<WorkPolicyResponse> = runCatching {
        val response = policyApi.updatePolicy(policyId, request)
        if (!response.isSuccessful) {
            val body = response.errorBody()?.string().orEmpty()
            if (response.code() == 401 || response.code() == 403) {
                throw AuthExpiredException("Session expired (${response.code()})")
            }
            throw IllegalStateException("Update policy request failed (${response.code()}): $body")
        }
        response.body() ?: throw IllegalStateException("Update policy response is empty")
    }
}
