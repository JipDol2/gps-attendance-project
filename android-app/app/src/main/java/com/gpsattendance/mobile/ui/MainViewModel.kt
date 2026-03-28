package com.gpsattendance.mobile.ui

import com.gpsattendance.mobile.data.model.AttendanceHistoryItemResponse
import com.gpsattendance.mobile.data.model.DashboardSummaryResponse
import com.gpsattendance.mobile.data.model.PermissionMemberResponse
import com.gpsattendance.mobile.data.model.PermissionResponse
import com.gpsattendance.mobile.data.model.TeamAttendanceTodayResponse
import com.gpsattendance.mobile.data.model.WorkPolicyResponse
import com.gpsattendance.mobile.data.model.WorkPolicyUpsertRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gpsattendance.mobile.data.model.WorkSessionResponse
import com.gpsattendance.mobile.data.repository.AuthExpiredException
import com.gpsattendance.mobile.data.repository.AttendanceRepository
import com.gpsattendance.mobile.data.repository.DashboardRepository
import com.gpsattendance.mobile.data.repository.PermissionRepository
import com.gpsattendance.mobile.data.repository.PolicyRepository
import com.gpsattendance.mobile.data.repository.TeamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val dashboardRepository: DashboardRepository,
    private val teamRepository: TeamRepository,
    private val policyRepository: PolicyRepository,
    private val permissionRepository: PermissionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun refreshSessions() {
        refreshVisibleSessions()
    }

    fun refreshVisibleSessions(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            } else {
                _uiState.value = _uiState.value.copy(error = null)
            }
            attendanceRepository.visibleSessions()
                .onSuccess { sessions ->
                    val members = buildVisibleMembers(sessions)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        sessions = sessions,
                        memberPins = buildMemberPins(members),
                        visibleMembers = members
                    )
                }
                .onFailure {
                    if (it is AuthExpiredException) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            authExpired = true,
                            error = "Session expired. Please login again."
                        )
                        return@onFailure
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = it.message ?: "Failed to load team sessions"
                    )
                }
        }
    }

    fun updateLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                error = null,
                myLatitude = latitude,
                myLongitude = longitude
            )
            attendanceRepository.updateMyLocation(latitude, longitude)
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(
                        trackingMessage = "${result.state}: ${result.message}"
                    )
                    refreshVisibleSessions(showLoading = false)
                }
                .onFailure {
                    if (it is AuthExpiredException) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            authExpired = true,
                            error = "Session expired. Please login again."
                        )
                        return@onFailure
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = it.message ?: "Failed to update location"
                    )
                }
        }
    }

    fun loadDashboardSummary() {
        viewModelScope.launch {
            dashboardRepository.summary()
                .onSuccess { summary ->
                    _uiState.value = _uiState.value.copy(dashboardSummary = summary, error = null)
                }
                .onFailure { handleFailure(it, "Failed to load dashboard summary") }
        }
    }

    fun loadAttendanceHistory(month: String? = null) {
        viewModelScope.launch {
            attendanceRepository.myAttendanceHistory(month)
                .onSuccess { history ->
                    _uiState.value = _uiState.value.copy(attendanceHistory = history, error = null)
                }
                .onFailure { handleFailure(it, "Failed to load attendance history") }
        }
    }

    fun loadTeamAttendanceToday(teamId: Long) {
        viewModelScope.launch {
            teamRepository.teamAttendanceToday(teamId)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(teamAttendanceToday = response, error = null)
                }
                .onFailure { handleFailure(it, "Failed to load team attendance") }
        }
    }

    fun loadWorkPolicies() {
        viewModelScope.launch {
            policyRepository.policies()
                .onSuccess { policies ->
                    _uiState.value = _uiState.value.copy(workPolicies = policies, error = null)
                }
                .onFailure { handleFailure(it, "Failed to load work policies") }
        }
    }

    fun loadPermissions() {
        viewModelScope.launch {
            permissionRepository.permissions()
                .onSuccess { permissions ->
                    val selectedId = _uiState.value.selectedPermissionId ?: permissions.firstOrNull()?.permissionId
                    _uiState.value = _uiState.value.copy(
                        permissions = permissions,
                        selectedPermissionId = selectedId,
                        error = null
                    )
                    selectedId?.let { loadPermissionMembers(it) }
                }
                .onFailure { handleFailure(it, "Failed to load permissions") }
        }
    }

    fun selectPermission(permissionId: Long) {
        _uiState.value = _uiState.value.copy(selectedPermissionId = permissionId)
        loadPermissionMembers(permissionId)
    }

    fun loadPermissionMembers(permissionId: Long) {
        viewModelScope.launch {
            permissionRepository.permissionMembers(permissionId)
                .onSuccess { members ->
                    _uiState.value = _uiState.value.copy(permissionMembers = members, error = null)
                }
                .onFailure { handleFailure(it, "Failed to load permission members") }
        }
    }

    fun createPermission(name: String, description: String?) {
        viewModelScope.launch {
            permissionRepository.createPermission(name = name.trim(), description = description?.trim()?.ifBlank { null })
                .onSuccess {
                    loadPermissions()
                }
                .onFailure { handleFailure(it, "Failed to create permission") }
        }
    }

    fun assignPermissionMember(permissionId: Long, userId: Long) {
        viewModelScope.launch {
            permissionRepository.assignPermissionMember(permissionId, userId)
                .onSuccess {
                    loadPermissionMembers(permissionId)
                    loadPermissions()
                }
                .onFailure { handleFailure(it, "Failed to assign member") }
        }
    }

    fun unassignPermissionMember(permissionId: Long, userId: Long) {
        viewModelScope.launch {
            permissionRepository.unassignPermissionMember(permissionId, userId)
                .onSuccess {
                    loadPermissionMembers(permissionId)
                    loadPermissions()
                }
                .onFailure { handleFailure(it, "Failed to unassign member") }
        }
    }

    fun updateWorkPolicy(
        policyId: Long,
        teamId: Long,
        workAddress: String,
        allowedRadiusM: Int,
        graceMinutes: Int,
        coreTimeStart: String,
        coreTimeEnd: String,
        enabled: Boolean
    ) {
        viewModelScope.launch {
            policyRepository.updatePolicy(
                policyId = policyId,
                request = WorkPolicyUpsertRequest(
                    teamId = teamId,
                    workAddress = workAddress,
                    allowedRadiusM = allowedRadiusM,
                    graceMinutes = graceMinutes,
                    coreTimeStart = coreTimeStart,
                    coreTimeEnd = coreTimeEnd,
                    enabled = enabled
                )
            ).onSuccess {
                loadWorkPolicies()
            }.onFailure { handleFailure(it, "Failed to update work policy") }
        }
    }

    fun consumeAuthExpired() {
        if (_uiState.value.authExpired) {
            _uiState.value = _uiState.value.copy(authExpired = false)
        }
    }

    fun setMyLocation(latitude: Double, longitude: Double) {
        _uiState.value = _uiState.value.copy(myLatitude = latitude, myLongitude = longitude)
    }

    private fun handleFailure(throwable: Throwable, fallback: String) {
        if (throwable is AuthExpiredException) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                authExpired = true,
                error = "Session expired. Please login again."
            )
            return
        }
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = throwable.message ?: fallback
        )
    }

    private fun buildVisibleMembers(sessions: List<WorkSessionResponse>): List<VisibleMember> {
        return sessions
            .asSequence()
            .groupBy { it.userId }
            .mapNotNull { (_, userSessions) ->
                val latest = userSessions.maxByOrNull {
                    listOf(it.checkInAt, it.checkOutAt, it.outsideSince).filterNotNull().maxOrNull().orEmpty()
                } ?: return@mapNotNull null
                val isCheckedIn = latest.status.equals("CHECKED_IN", ignoreCase = true)
                val isInRange = isCheckedIn && latest.outsideSince == null
                VisibleMember(
                    userId = latest.userId,
                    userName = latest.userName,
                    status = latest.status,
                    isCheckedIn = isCheckedIn,
                    isInRange = isInRange,
                    latitude = latest.lastLatitude,
                    longitude = latest.lastLongitude
                )
            }
            .toList()
    }

    private fun buildMemberPins(members: List<VisibleMember>): List<TeamMemberPin> {
        return members
            .asSequence()
            .filter { it.latitude != null && it.longitude != null }
            .map {
                TeamMemberPin(
                    userId = it.userId,
                    userName = it.userName,
                    status = it.status,
                    isCheckedIn = it.isCheckedIn,
                    isInRange = it.isInRange,
                    latitude = it.latitude ?: return@map null,
                    longitude = it.longitude ?: return@map null
                )
            }
            .filterNotNull()
            .toList()
    }
}

data class TeamMemberPin(
    val userId: Long,
    val userName: String,
    val status: String,
    val isCheckedIn: Boolean,
    val isInRange: Boolean,
    val latitude: Double,
    val longitude: Double
)

data class VisibleMember(
    val userId: Long,
    val userName: String,
    val status: String,
    val isCheckedIn: Boolean,
    val isInRange: Boolean,
    val latitude: Double?,
    val longitude: Double?
)

data class MainUiState(
    val isLoading: Boolean = false,
    val sessions: List<WorkSessionResponse> = emptyList(),
    val memberPins: List<TeamMemberPin> = emptyList(),
    val visibleMembers: List<VisibleMember> = emptyList(),
    val dashboardSummary: DashboardSummaryResponse? = null,
    val attendanceHistory: List<AttendanceHistoryItemResponse> = emptyList(),
    val teamAttendanceToday: TeamAttendanceTodayResponse? = null,
    val workPolicies: List<WorkPolicyResponse> = emptyList(),
    val permissions: List<PermissionResponse> = emptyList(),
    val selectedPermissionId: Long? = null,
    val permissionMembers: List<PermissionMemberResponse> = emptyList(),
    val myLatitude: Double? = null,
    val myLongitude: Double? = null,
    val trackingMessage: String? = null,
    val error: String? = null,
    val authExpired: Boolean = false
)
