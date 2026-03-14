package com.gpsattendance.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gpsattendance.mobile.BuildConfig
import com.gpsattendance.mobile.data.model.TeamResponse
import com.gpsattendance.mobile.data.repository.AuthRepository
import com.gpsattendance.mobile.data.repository.TeamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val teamRepository: TeamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    init {
        loadSession()
        loadTeams()
    }

    fun loadSession() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isLoggedIn = authRepository.isLoggedIn(),
                userName = authRepository.currentUserName()
            )
        }
    }

    fun loadTeams() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTeamsLoading = true, teamsError = null)
            teamRepository.teams()
                .onSuccess { teams ->
                    _uiState.value = _uiState.value.copy(isTeamsLoading = false, teams = teams)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isTeamsLoading = false,
                        teamsError = it.message ?: "팀 목록 조회 실패"
                    )
                }
        }
    }

    fun register(loginId: String, email: String, password: String, name: String, teamId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                infoMessage = null,
                registrationCompleted = false
            )
            authRepository.register(loginId, email, password, name, teamId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        infoMessage = "Registration complete. Please login.",
                        registrationCompleted = true
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = toUserMessage(it, "Registration failed"),
                        registrationCompleted = false
                    )
                }
        }
    }

    fun login(loginId: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                infoMessage = null,
                registrationCompleted = false
            )
            authRepository.login(loginId, password)
                .onSuccess {
                    _uiState.value = SessionUiState(
                        isLoading = false,
                        isLoggedIn = true,
                        userName = authRepository.currentUserName()
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = toUserMessage(it, "Login failed")
                    )
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = SessionUiState(isLoading = false)
        }
    }

    private fun toUserMessage(throwable: Throwable, fallback: String): String {
        val chain = generateSequence(throwable) { it.cause }.toList()
        val rawMessage = chain.joinToString(" | ") { it.message.orEmpty() }

        return when {
            chain.any { it is ConnectException } || rawMessage.contains("failed to connect", ignoreCase = true) -> {
                "Cannot connect to server (${BuildConfig.BASE_URL}). If this is a real device, use your PC LAN IP or run adb reverse tcp:8080 tcp:8080."
            }

            chain.any { it is UnknownHostException } -> {
                "Cannot resolve server host (${BuildConfig.BASE_URL}). Check BASE_URL host and network."
            }

            chain.any { it is SSLException } -> {
                "SSL/TLS handshake failed. Check HTTPS certificate and server TLS config."
            }

            else -> throwable.message ?: fallback
        }
    }
}

data class SessionUiState(
    val isLoading: Boolean = true,
    val isLoggedIn: Boolean = false,
    val userName: String? = null,
    val teams: List<TeamResponse> = emptyList(),
    val isTeamsLoading: Boolean = false,
    val teamsError: String? = null,
    val error: String? = null,
    val infoMessage: String? = null,
    val registrationCompleted: Boolean = false
)
