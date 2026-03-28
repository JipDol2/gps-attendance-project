package com.gpsattendance.mobile.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gpsattendance.mobile.GpsAttendanceApp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelTextBuilder
import com.kakao.vectormap.label.LabelTextStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val AppBackground = Color(0xFFF2F4F8)
private val AppCardBorder = Color(0xFFDDE3EA)
private val AppPrimary = Color(0xFF0B132B)
private val AppAccent = Color(0xFF2563EB)
private val AppTextMuted = Color(0xFF6B7280)

@Composable
fun AppScreen(
    sessionViewModel: SessionViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val sessionState by sessionViewModel.uiState.collectAsStateWithLifecycle()
    val mainState by mainViewModel.uiState.collectAsStateWithLifecycle()

    if (sessionState.isLoggedIn) {
        LaunchedEffect(Unit) {
            mainViewModel.refreshVisibleSessions()
            mainViewModel.loadDashboardSummary()
            mainViewModel.loadAttendanceHistory()
            mainViewModel.loadWorkPolicies()
            mainViewModel.loadPermissions()
        }

        LaunchedEffect(mainState.authExpired) {
            if (mainState.authExpired) {
                sessionViewModel.logout()
                mainViewModel.consumeAuthExpired()
            }
        }

        MainHomeContent(
            userName = sessionState.userName,
            state = mainState,
            onLogout = sessionViewModel::logout,
            onUpdateLocation = mainViewModel::updateLocation,
            onMyLocationDetected = mainViewModel::setMyLocation,
            onLoadDashboard = mainViewModel::loadDashboardSummary,
            onLoadAttendance = mainViewModel::loadAttendanceHistory,
            onLoadTeamAttendance = mainViewModel::loadTeamAttendanceToday,
            onLoadPolicies = mainViewModel::loadWorkPolicies,
            onLoadPermissions = mainViewModel::loadPermissions,
            onSelectPermission = mainViewModel::selectPermission,
            onCreatePermission = mainViewModel::createPermission,
            onAssignPermissionMember = mainViewModel::assignPermissionMember,
            onUnassignPermissionMember = mainViewModel::unassignPermissionMember,
            onUpdatePolicy = mainViewModel::updateWorkPolicy
        )
    } else {
        LoginContent(
            state = sessionState,
            onLogin = sessionViewModel::login,
            onRegister = sessionViewModel::register,
            onRefreshTeams = sessionViewModel::loadTeams
        )
    }
}

@Composable
private fun LoginContent(
    state: SessionUiState,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String, String, Long) -> Unit,
    onRefreshTeams: () -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var loginId by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedTeamId by remember { mutableStateOf<Long?>(null) }
    var teamMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.registrationCompleted) {
        if (state.registrationCompleted) {
            isRegisterMode = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 430.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppCardBorder)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .background(AppAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⌖", color = Color.White, fontSize = 24.sp)
                    }
                }
                Text(
                    "GPS Attendance",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Text(
                    "GPS 기반 자동 출퇴근 관리 시스템",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTextMuted,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = loginId,
                    onValueChange = { loginId = it },
                    label = { Text(if (isRegisterMode) "아이디" else "로그인 ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("비밀번호") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                if (isRegisterMode) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("이메일") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("이름") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { teamMenuExpanded = true },
                            enabled = !state.isTeamsLoading && state.teams.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            val teamName = state.teams.firstOrNull { it.id == selectedTeamId }?.name
                            Text(teamName ?: if (state.isTeamsLoading) "팀 불러오는 중..." else "팀 선택")
                        }
                        DropdownMenu(
                            expanded = teamMenuExpanded,
                            onDismissRequest = { teamMenuExpanded = false }
                        ) {
                            state.teams.forEach { team ->
                                DropdownMenuItem(
                                    text = { Text(team.name) },
                                    onClick = {
                                        selectedTeamId = team.id
                                        teamMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    state.teamsError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onRefreshTeams, enabled = !state.isTeamsLoading) {
                            Text("팀 목록 다시 불러오기")
                        }
                    }
                }

                Button(
                    onClick = {
                        if (isRegisterMode) {
                            onRegister(
                                loginId.trim(),
                                email.trim(),
                                password,
                                name.trim(),
                                selectedTeamId ?: return@Button
                            )
                        } else {
                            onLogin(loginId.trim(), password)
                        }
                    },
                    enabled = !state.isLoading && loginId.isNotBlank() && password.isNotBlank() &&
                        (!isRegisterMode || (name.isNotBlank() &&
                            email.isNotBlank() &&
                            password.length >= 8 &&
                            selectedTeamId != null &&
                            state.teams.isNotEmpty())),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isRegisterMode) "회원가입" else "로그인")
                }

                TextButton(
                    onClick = {
                        isRegisterMode = !isRegisterMode
                        if (isRegisterMode) onRefreshTeams()
                    },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isRegisterMode) "로그인으로 돌아가기" else "계정 만들기")
                }

                state.infoMessage?.let {
                    Text(it, color = AppAccent, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }

                state.error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

private enum class MainMenu(val label: String) {
    DASHBOARD("대시보드"),
    ATTENDANCE("출퇴근 기록"),
    LOCATION_SHARE("위치 공유"),
    TEAM("팀 관리"),
    POLICY("근무 정책"),
    PERMISSION("권한 관리")
}

@Composable
private fun MainHomeContent(
    userName: String?,
    state: MainUiState,
    onLogout: () -> Unit,
    onUpdateLocation: (Double, Double) -> Unit,
    onMyLocationDetected: (Double, Double) -> Unit,
    onLoadDashboard: () -> Unit,
    onLoadAttendance: (String?) -> Unit,
    onLoadTeamAttendance: (Long) -> Unit,
    onLoadPolicies: () -> Unit,
    onLoadPermissions: () -> Unit,
    onSelectPermission: (Long) -> Unit,
    onCreatePermission: (String, String?) -> Unit,
    onAssignPermissionMember: (Long, Long) -> Unit,
    onUnassignPermissionMember: (Long, Long) -> Unit,
    onUpdatePolicy: (Long, Long, String, Int, Int, String, String, Boolean) -> Unit
) {
    var selectedMenu by rememberSaveable { mutableStateOf(MainMenu.DASHBOARD) }

    LaunchedEffect(selectedMenu, state.dashboardSummary?.teamId) {
        when (selectedMenu) {
            MainMenu.DASHBOARD -> onLoadDashboard()
            MainMenu.ATTENDANCE -> onLoadAttendance(null)
            MainMenu.TEAM -> state.dashboardSummary?.teamId?.let(onLoadTeamAttendance)
            MainMenu.POLICY -> onLoadPolicies()
            MainMenu.PERMISSION -> onLoadPermissions()
            MainMenu.LOCATION_SHARE -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppCardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("GPS Attendance", style = MaterialTheme.typography.titleMedium, color = AppPrimary)
                    Text("자동 출퇴근 관리", style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(userName ?: "사용자", style = MaterialTheme.typography.labelMedium, color = AppPrimary)
                    TextButton(onClick = onLogout, modifier = Modifier.height(32.dp)) { Text("로그아웃") }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MainMenuChip(MainMenu.DASHBOARD, selectedMenu, Modifier.weight(1f)) { selectedMenu = it }
                MainMenuChip(MainMenu.ATTENDANCE, selectedMenu, Modifier.weight(1f)) { selectedMenu = it }
                MainMenuChip(MainMenu.LOCATION_SHARE, selectedMenu, Modifier.weight(1f)) { selectedMenu = it }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MainMenuChip(MainMenu.TEAM, selectedMenu, Modifier.weight(1f)) { selectedMenu = it }
                MainMenuChip(MainMenu.POLICY, selectedMenu, Modifier.weight(1f)) { selectedMenu = it }
                MainMenuChip(MainMenu.PERMISSION, selectedMenu, Modifier.weight(1f)) { selectedMenu = it }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedMenu) {
                MainMenu.DASHBOARD -> DashboardMobileContent(state)
                MainMenu.ATTENDANCE -> AttendanceMobileContent(state)
                MainMenu.LOCATION_SHARE -> MapHomeContent(
                    state = state,
                    onUpdateLocation = onUpdateLocation,
                    onMyLocationDetected = onMyLocationDetected
                )
                MainMenu.TEAM -> TeamMobileContent(state)
                MainMenu.POLICY -> PolicyMobileContent(
                    state = state,
                    onUpdatePolicy = onUpdatePolicy
                )
                MainMenu.PERMISSION -> PermissionMobileContent(
                    state = state,
                    onSelectPermission = onSelectPermission,
                    onCreatePermission = onCreatePermission,
                    onAssignPermissionMember = onAssignPermissionMember,
                    onUnassignPermissionMember = onUnassignPermissionMember
                )
            }
        }
    }
}

@Composable
private fun MainMenuChip(
    menu: MainMenu,
    selectedMenu: MainMenu,
    modifier: Modifier = Modifier,
    onSelected: (MainMenu) -> Unit
) {
    val selected = menu == selectedMenu
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) AppAccent else Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) AppAccent else AppCardBorder)
    ) {
        TextButton(onClick = { onSelected(menu) }, modifier = Modifier.fillMaxWidth()) {
            Text(menu.label, color = if (selected) Color.White else AppPrimary, maxLines = 1)
        }
    }
}

@Composable
private fun DashboardMobileContent(state: MainUiState) {
    val summary = state.dashboardSummary
    val total = summary?.totalMembers ?: state.visibleMembers.size
    val checkedIn = summary?.checkedInMembers ?: state.visibleMembers.count { it.isCheckedIn }
    val notCheckedIn = summary?.notCheckedInMembers ?: (total - checkedIn).coerceAtLeast(0)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("오늘의 출퇴근 현황", style = MaterialTheme.typography.titleMedium, color = AppPrimary)
                    Text(
                        "${summary?.teamName ?: "팀 미지정"} · ${summary?.workDate ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTextMuted
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        StatusPill("전체", "${total}명", Modifier.weight(1f))
                        StatusPill("출근", "${checkedIn}명", Modifier.weight(1f))
                        StatusPill("미출근", "${notCheckedIn}명", Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("빠른 메뉴", style = MaterialTheme.typography.titleMedium, color = AppPrimary)
                    Text("위치 공유는 메뉴에서 선택 시 카카오지도가 열립니다.", color = AppTextMuted, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ActionMenuItem("출퇴근 기록", "⏱", "월간 현황", Modifier.weight(1f))
                        ActionMenuItem("위치 공유", "⌖", "지도 보기", Modifier.weight(1f))
                        ActionMenuItem("팀 관리", "👥", "팀원 상태", Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceMobileContent(state: MainUiState) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("최근 출퇴근 기록", style = MaterialTheme.typography.titleMedium, color = AppPrimary)
            if (state.attendanceHistory.isEmpty()) {
                Text("아직 기록이 없습니다.", color = AppTextMuted)
            } else {
                state.attendanceHistory.take(10).forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.workDate ?: "-", color = AppPrimary)
                        Text(
                            item.status ?: "-",
                            color = if ((item.status ?: "").contains("CHECKED_IN")) Color(0xFF16A34A) else AppTextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamMobileContent(state: MainUiState) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppCardBorder)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("팀원 출퇴근 현황", style = MaterialTheme.typography.titleMedium, color = AppPrimary)
            val teamInfo = state.teamAttendanceToday
            if (teamInfo == null || teamInfo.members.isEmpty()) {
                Text("표시할 팀 정보가 없습니다.", color = AppTextMuted)
            } else {
                Text("${teamInfo.teamName} · ${teamInfo.workDate ?: ""}", color = AppTextMuted, style = MaterialTheme.typography.bodySmall)
                teamInfo.members.forEach { member ->
                    val locationText = if (member.locationKnown) {
                        "위치 확인"
                    } else {
                        "위치 미확인"
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(member.userName, color = AppPrimary)
                        Text("${member.status ?: "-"} · $locationText", color = AppTextMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun PolicyMobileContent(
    state: MainUiState,
    onUpdatePolicy: (Long, Long, String, Int, Int, String, String, Boolean) -> Unit
) {
    var selectedPolicyId by remember { mutableStateOf<Long?>(null) }
    val selectedPolicy = state.workPolicies.firstOrNull { it.policyId == selectedPolicyId } ?: state.workPolicies.firstOrNull()
    var address by remember(selectedPolicy?.policyId) { mutableStateOf(selectedPolicy?.workAddress.orEmpty()) }
    var radiusText by remember(selectedPolicy?.policyId) { mutableStateOf((selectedPolicy?.allowedRadiusM ?: 200).toString()) }
    var graceText by remember(selectedPolicy?.policyId) { mutableStateOf((selectedPolicy?.graceMinutes ?: 5).toString()) }
    var coreStart by remember(selectedPolicy?.policyId) { mutableStateOf(selectedPolicy?.coreTimeStart ?: "10:00") }
    var coreEnd by remember(selectedPolicy?.policyId) { mutableStateOf(selectedPolicy?.coreTimeEnd ?: "16:00") }

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppCardBorder)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("근무 정책 안내", style = MaterialTheme.typography.titleMedium, color = AppPrimary)
            if (state.workPolicies.isEmpty()) {
                Text("정책 데이터가 없습니다.", color = AppTextMuted)
            } else {
                state.workPolicies.forEach { policy ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFD)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppCardBorder)
                    ) {
                        TextButton(onClick = { selectedPolicyId = policy.policyId }, modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(policy.teamName ?: "팀", color = AppPrimary, fontWeight = FontWeight.SemiBold)
                                Text("${policy.allowedRadiusM}m / ${policy.graceMinutes}분", color = AppTextMuted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                selectedPolicy?.let { policy ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("선택 정책 수정", color = AppPrimary, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("근무지") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = radiusText,
                            onValueChange = { radiusText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("허용반경(m)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = graceText,
                            onValueChange = { graceText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("유예(분)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = coreStart,
                            onValueChange = { coreStart = it },
                            label = { Text("코어 시작") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = coreEnd,
                            onValueChange = { coreEnd = it },
                            label = { Text("코어 종료") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Button(
                        onClick = {
                            val radius = radiusText.toIntOrNull() ?: return@Button
                            val grace = graceText.toIntOrNull() ?: return@Button
                            onUpdatePolicy(
                                policy.policyId,
                                policy.teamId,
                                address,
                                radius,
                                grace,
                                coreStart,
                                coreEnd,
                                policy.enabled
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("정책 저장")
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionMobileContent(
    state: MainUiState,
    onSelectPermission: (Long) -> Unit,
    onCreatePermission: (String, String?) -> Unit,
    onAssignPermissionMember: (Long, Long) -> Unit,
    onUnassignPermissionMember: (Long, Long) -> Unit
) {
    var newPermissionName by remember { mutableStateOf("") }
    var newPermissionDesc by remember { mutableStateOf("") }
    var memberUserIdText by remember { mutableStateOf("") }
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppCardBorder)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("권한 관리", style = MaterialTheme.typography.titleMedium, color = AppPrimary)
            OutlinedTextField(
                value = newPermissionName,
                onValueChange = { newPermissionName = it },
                label = { Text("권한명") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = newPermissionDesc,
                onValueChange = { newPermissionDesc = it },
                label = { Text("설명") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Button(
                onClick = {
                    if (newPermissionName.isNotBlank()) {
                        onCreatePermission(newPermissionName, newPermissionDesc.ifBlank { null })
                        newPermissionName = ""
                        newPermissionDesc = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("권한 생성")
            }
            if (state.permissions.isEmpty()) {
                Text("권한 데이터가 없습니다.", color = AppTextMuted)
            } else {
                state.permissions.forEach { permission ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (state.selectedPermissionId == permission.permissionId) Color(0xFFEFF4FF) else Color(0xFFF8FAFD)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppCardBorder)
                    ) {
                        TextButton(
                            onClick = { onSelectPermission(permission.permissionId) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(permission.name, color = AppPrimary)
                                Text("${permission.memberCount}명", color = AppTextMuted)
                            }
                        }
                    }
                }
                Text("구성원", color = AppPrimary, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = memberUserIdText,
                        onValueChange = { memberUserIdText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("유저 ID") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            val permissionId = state.selectedPermissionId ?: return@Button
                            val userId = memberUserIdText.toLongOrNull() ?: return@Button
                            onAssignPermissionMember(permissionId, userId)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("멤버 할당") }
                    Button(
                        onClick = {
                            val permissionId = state.selectedPermissionId ?: return@Button
                            val userId = memberUserIdText.toLongOrNull() ?: return@Button
                            onUnassignPermissionMember(permissionId, userId)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("멤버 해제") }
                }
                if (state.permissionMembers.isEmpty()) {
                    Text("선택한 권한의 구성원이 없습니다.", color = AppTextMuted, style = MaterialTheme.typography.bodySmall)
                } else {
                    state.permissionMembers.forEach { member ->
                        Text("${member.userName} (${member.teamName ?: "-"})", color = AppTextMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun MapHomeContent(
    state: MainUiState,
    onUpdateLocation: (Double, Double) -> Unit,
    onMyLocationDetected: (Double, Double) -> Unit
) {
    if (!GpsAttendanceApp.isKakaoMapAvailable) {
        UnsupportedMapContent(userName = null, state = state, onLogout = {})
        return
    }

    val context = LocalContext.current
    val mapView = rememberKakaoMapViewWithLifecycle()
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var mapError by remember { mutableStateOf<String?>(null) }
    var hasInitiallyCentered by rememberSaveable { mutableStateOf(false) }
    val hasLocationPermission = hasFineLocationPermission(context)
    var requestedLocationPermission by remember { mutableStateOf(false) }
    val fabBottomPadding: Dp by animateDpAsState(targetValue = 16.dp, label = "fabBottomPadding")

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fetchCurrentLocation(context) { lat, lng ->
                onMyLocationDetected(lat, lng)
                onUpdateLocation(lat, lng)
            }
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            fetchCurrentLocation(context) { lat, lng ->
                onMyLocationDetected(lat, lng)
                onUpdateLocation(lat, lng)
            }
        } else if (!requestedLocationPermission) {
            requestedLocationPermission = true
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) return@LaunchedEffect
        while (isActive) {
            fetchCurrentLocation(context) { lat, lng ->
                onMyLocationDetected(lat, lng)
                onUpdateLocation(lat, lng)
            }
            delay(5_000)
        }
    }

    DisposableEffect(mapView) {
        mapView.start(
            object : MapLifeCycleCallback() {
                override fun onMapDestroy() = Unit
                override fun onMapError(exception: Exception) {
                    mapError = exception.message ?: "Failed to initialize Kakao map"
                }
            },
            object : KakaoMapReadyCallback() {
                override fun onMapReady(map: KakaoMap) {
                    kakaoMap = map
                    mapError = null
                }

                override fun getPosition(): LatLng {
                    return LatLng.from(37.5665, 126.9780)
                }

                override fun getZoomLevel(): Int {
                    return 12
                }
            }
        )
        onDispose {
            mapView.finish()
            kakaoMap = null
        }
    }

    LaunchedEffect(state.myLatitude, state.myLongitude, kakaoMap, hasInitiallyCentered) {
        if (hasInitiallyCentered) return@LaunchedEffect
        val lat = state.myLatitude ?: return@LaunchedEffect
        val lng = state.myLongitude ?: return@LaunchedEffect
        kakaoMap?.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(lat, lng), 15))
        hasInitiallyCentered = true
    }

    LaunchedEffect(kakaoMap, state.memberPins, state.myLatitude, state.myLongitude) {
        val map = kakaoMap ?: return@LaunchedEffect
        runCatching {
            renderMemberLabels(
                map = map,
                members = state.memberPins,
                myLatitude = state.myLatitude,
                myLongitude = state.myLongitude
            )
        }.onFailure {
            mapError = it.message ?: "Failed to render map labels"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { mapView }
            )

            FloatingActionButton(
                onClick = {
                    val lat = state.myLatitude
                    val lng = state.myLongitude
                    if (lat != null && lng != null) {
                        kakaoMap?.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(lat, lng), 16))
                    } else if (hasFineLocationPermission(context)) {
                        fetchCurrentLocation(context) { myLat, myLng ->
                            onMyLocationDetected(myLat, myLng)
                            onUpdateLocation(myLat, myLng)
                            kakaoMap?.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(myLat, myLng), 16))
                        }
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = fabBottomPadding)
                    .height(46.dp)
            ) {
                Text("현재 위치")
            }

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            val overlayError = mapError ?: state.error
            overlayError?.let {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 96.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TopSearchLikeBar(
    userName: String?,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    onClick = onMenuClick,
                    modifier = Modifier
                        .height(34.dp)
                        .background(Color(0xFFF4F6FA), RoundedCornerShape(10.dp))
                ) {
                    Text("☰", fontSize = 17.sp, color = AppPrimary)
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "GPS Attendance",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = AppPrimary
                    )
                    Text(
                        text = "자동 출퇴근 관리",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppTextMuted
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFF4F6FA), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = userName ?: "사용자",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = AppPrimary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(Color(0xFFE8EEFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⌖", color = AppAccent, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun QuickActionGridPanel(
    state: MainUiState,
    modifier: Modifier = Modifier
) {
    val checkedInCount = state.visibleMembers.count { it.isCheckedIn }
    val totalCount = state.visibleMembers.size
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.97f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "오늘 출퇴근 현황",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = AppPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatusPill("전체", "${totalCount}명", Modifier.weight(1f))
                StatusPill("출근", "${checkedInCount}명", Modifier.weight(1f))
                StatusPill("미출근", "${(totalCount - checkedInCount).coerceAtLeast(0)}명", Modifier.weight(1f))
            }
            Text(
                text = "빠른 메뉴",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = AppPrimary
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionMenuItem(
                    label = "출퇴근 기록",
                    icon = "⏱",
                    subLabel = "최근 기록 확인",
                    modifier = Modifier.weight(1f)
                )
                ActionMenuItem(
                    label = "위치 공유",
                    icon = "⌖",
                    subLabel = "현재 카카오지도 사용",
                    modifier = Modifier.weight(1f)
                )
                ActionMenuItem(
                    label = "팀 관리",
                    icon = "👥",
                    subLabel = "팀 현황 확인",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ActionMenuItem(
    label: String,
    icon: String,
    subLabel: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .border(
                width = 1.dp,
                color = AppCardBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .background(Color(0xFFFCFDFF), RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(Color(0xFFE9EEFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 14.sp, color = AppAccent)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                color = AppPrimary
            ),
            textAlign = TextAlign.Center
        )
        Text(
            text = subLabel,
            style = MaterialTheme.typography.labelSmall,
            color = AppTextMuted,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatusPill(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FC)),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
            Text(value, style = MaterialTheme.typography.titleSmall, color = AppPrimary, fontWeight = FontWeight.SemiBold)
        }
    }
}

private enum class BottomTab {
    HOME,
    TRANSIT,
    NAVI,
    AROUND,
    BOOKMARK,
    MY;

    val label: String
        get() = when (this) {
            HOME -> "홈"
            TRANSIT -> "출퇴근 기록"
            NAVI -> "근무지 정보"
            AROUND -> "팀 관리"
            BOOKMARK -> "근무 정책"
            MY -> "마이"
        }
}

@Composable
private fun MySheetPanel(
    userName: String?,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = userName ?: "로그인 사용자",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "계정 및 권한 설정",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTextMuted
            )
            Button(onClick = onLogout, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text("로그아웃")
            }
        }
    }
}

@Composable
private fun PlaceholderSheetPanel(
    title: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("$title 메뉴는 준비 중입니다", style = MaterialTheme.typography.titleMedium, color = AppTextMuted)
        }
    }
}

@Composable
private fun BottomNavBar(
    selectedTab: BottomTab,
    onSelected: (BottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomTabItem("홈", "⌂", selectedTab == BottomTab.HOME) { onSelected(BottomTab.HOME) }
            BottomTabItem("기록", "⏱", selectedTab == BottomTab.TRANSIT) { onSelected(BottomTab.TRANSIT) }
            BottomTabItem("근무", "⌖", selectedTab == BottomTab.NAVI) { onSelected(BottomTab.NAVI) }
            BottomTabItem("팀", "👥", selectedTab == BottomTab.AROUND) { onSelected(BottomTab.AROUND) }
            BottomTabItem("정책", "⚙", selectedTab == BottomTab.BOOKMARK) { onSelected(BottomTab.BOOKMARK) }
            BottomTabItem("마이", "◯", selectedTab == BottomTab.MY) { onSelected(BottomTab.MY) }
        }
    }
}

@Composable
private fun BottomTabItem(
    label: String,
    icon: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick, modifier = Modifier.height(52.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, color = if (selected) AppAccent else Color(0xFF70757D), fontSize = 14.sp)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) AppAccent else Color(0xFF70757D),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MemberListContent(
    members: List<VisibleMember>,
    isLoading: Boolean
) {
    if (isLoading && members.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (members.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("표시 가능한 팀원 데이터가 없습니다.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(members, key = { it.userId }) { member ->
            val inRange = member.isInRange
            val cardColor = if (inRange) Color(0xFFE8F5E9) else Color(0xFFE0E0E0)
            val statusText = when {
                member.isCheckedIn && inRange -> "근무중 (반경 내)"
                member.isCheckedIn -> "근무중 (반경 밖)"
                else -> "퇴근"
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(member.userName, style = MaterialTheme.typography.titleMedium)
                    Text(statusText)
                    Text("Session status: ${member.status}", style = MaterialTheme.typography.bodySmall)
                    if (member.latitude != null && member.longitude != null) {
                        Text(
                            "위치: %.5f, %.5f".format(member.latitude, member.longitude),
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Text("위치 정보 없음", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun UnsupportedMapContent(
    userName: String?,
    state: MainUiState,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Welcome, ${userName ?: "User"}", style = MaterialTheme.typography.titleMedium)
        Text(
            "현재 에뮬레이터 ABI(x86/x86_64)는 Kakao Map SDK를 지원하지 않아 지도가 비활성화되었습니다.",
            color = MaterialTheme.colorScheme.error
        )
        Text("ARM64 에뮬레이터 또는 실제 안드로이드 기기에서 실행해 주세요.")
        Text("Visible members: ${state.visibleMembers.size}")

        Button(onClick = onLogout) { Text("Logout") }

        if (state.isLoading) {
            CircularProgressIndicator()
        }

        state.trackingMessage?.let { Text(it) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        MemberListContent(members = state.visibleMembers, isLoading = state.isLoading)
    }
}

private fun renderMemberLabels(
    map: KakaoMap,
    members: List<TeamMemberPin>,
    myLatitude: Double?,
    myLongitude: Double?
) {
    val layer = map.labelManager?.layer ?: return
    layer.removeAll()

    members.forEach { member ->
        val labelColor = if (member.isInRange) AndroidColor.parseColor("#2E7D32") else AndroidColor.parseColor("#616161")
        val text = if (member.isInRange) "${member.userName} ●" else "${member.userName} ○"
        val options = LabelOptions.from(LatLng.from(member.latitude, member.longitude))
            .setStyles(
                LabelStyle.from(
                    LabelTextStyle.from(28, labelColor, 4, AndroidColor.WHITE)
                )
            )
            .setTexts(LabelTextBuilder().setTexts(text))
        layer.addLabel(options)
    }

    if (myLatitude != null && myLongitude != null) {
        val myOptions = LabelOptions.from(LatLng.from(myLatitude, myLongitude))
            .setStyles(
                LabelStyle.from(
                    LabelTextStyle.from(34, AndroidColor.parseColor("#D32F2F"), 3, AndroidColor.WHITE)
                )
            )
            .setTexts(LabelTextBuilder().setTexts("●"))
        layer.addLabel(myOptions)
    }
}

private fun hasFineLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

@SuppressLint("MissingPermission")
private fun fetchCurrentLocation(context: Context, onLocation: (Double, Double) -> Unit) {
    val client = LocationServices.getFusedLocationProviderClient(context)
    client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        .addOnSuccessListener { location ->
            if (location != null) onLocation(location.latitude, location.longitude)
        }
}

@Composable
private fun rememberKakaoMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                mapView.resume()
            }

            override fun onPause(owner: LifecycleOwner) {
                mapView.pause()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return mapView
}

