package com.gpsattendance.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gpsattendance.mobile.data.model.WorkSessionResponse
import com.gpsattendance.mobile.data.repository.AuthExpiredException
import com.gpsattendance.mobile.data.repository.AttendanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository
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

    fun consumeAuthExpired() {
        if (_uiState.value.authExpired) {
            _uiState.value = _uiState.value.copy(authExpired = false)
        }
    }

    fun setMyLocation(latitude: Double, longitude: Double) {
        _uiState.value = _uiState.value.copy(myLatitude = latitude, myLongitude = longitude)
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
    val myLatitude: Double? = null,
    val myLongitude: Double? = null,
    val trackingMessage: String? = null,
    val error: String? = null,
    val authExpired: Boolean = false
)
