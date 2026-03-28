package com.attendance.mobile.application;

import com.attendance.attendance.application.AttendanceQueryService;
import com.attendance.attendance.domain.WorkPolicy;
import com.attendance.attendance.domain.WorkSession;
import com.attendance.attendance.domain.WorkSessionStatus;
import com.attendance.attendance.infrastructure.WorkPolicyRepository;
import com.attendance.attendance.infrastructure.WorkSessionRepository;
import com.attendance.mobile.presentation.dto.AttendanceHistoryItemResponse;
import com.attendance.mobile.presentation.dto.DashboardSummaryResponse;
import com.attendance.mobile.presentation.dto.PermissionMemberResponse;
import com.attendance.mobile.presentation.dto.PermissionResponse;
import com.attendance.mobile.presentation.dto.TeamAttendanceMemberResponse;
import com.attendance.mobile.presentation.dto.TeamAttendanceTodayResponse;
import com.attendance.mobile.presentation.dto.WorkPolicyResponse;
import com.attendance.organization.domain.RoleLevel;
import com.attendance.organization.domain.Team;
import com.attendance.organization.infrastructure.TeamRepository;
import com.attendance.shared.exception.BusinessException;
import com.attendance.user.domain.User;
import com.attendance.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MobileQueryService {

    private final AttendanceQueryService attendanceQueryService;
    private final WorkSessionRepository workSessionRepository;
    private final WorkPolicyRepository workPolicyRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    public DashboardSummaryResponse dashboardSummary(String loginId) {
        User viewer = userRepository.findByLoginIdWithRelations(loginId)
                .orElseThrow(() -> new BusinessException("viewer user not found"));

        Page<WorkSession> visiblePage = attendanceQueryService.visibleSessionsByLoginId(
                loginId,
                null,
                null,
                Pageable.ofSize(1000)
        );

        Map<Long, WorkSession> latestByUser = latestByUser(visiblePage.getContent());
        int totalMembers = latestByUser.size();
        int checkedInMembers = (int) latestByUser.values().stream()
                .filter(session -> session.getStatus() == WorkSessionStatus.CHECKED_IN)
                .count();

        String myStatus = workSessionRepository.findByUserIdOrderByCheckInAtDesc(viewer.getId(), Pageable.ofSize(1))
                .stream()
                .findFirst()
                .map(session -> session.getStatus().name())
                .orElse("NOT_CHECKED_IN");

        return new DashboardSummaryResponse(
                viewer.getTeam() == null ? null : viewer.getTeam().getId(),
                viewer.getTeam() == null ? null : viewer.getTeam().getName(),
                LocalDate.now(),
                totalMembers,
                checkedInMembers,
                Math.max(totalMembers - checkedInMembers, 0),
                myStatus
        );
    }

    public Page<AttendanceHistoryItemResponse> myAttendanceHistory(Long userId, String month, Pageable pageable) {
        Page<WorkSession> sessions;
        if (month == null || month.isBlank()) {
            sessions = workSessionRepository.findByUserIdOrderByCheckInAtDesc(userId, pageable);
        } else {
            YearMonth yearMonth = YearMonth.parse(month);
            LocalDateTime from = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime to = yearMonth.plusMonths(1).atDay(1).atStartOfDay();
            sessions = workSessionRepository.findByUserIdAndCheckInAtBetweenOrderByCheckInAtDesc(userId, from, to, pageable);
        }
        return sessions.map(this::toAttendanceHistoryItem);
    }

    public TeamAttendanceTodayResponse teamAttendanceToday(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException("team not found"));

        List<User> users = userRepository.findByTeamId(teamId);
        if (users.isEmpty()) {
            return new TeamAttendanceTodayResponse(teamId, team.getName(), LocalDate.now(), List.of());
        }

        List<Long> userIds = users.stream().map(User::getId).toList();
        LocalDateTime from = LocalDate.now().atStartOfDay();
        LocalDateTime to = from.plusDays(1);

        List<WorkSession> todaySessions = workSessionRepository
                .findByUserIdInAndCheckInAtBetweenOrderByCheckInAtDesc(userIds, from, to);
        Map<Long, WorkSession> latestByUser = latestByUser(todaySessions);

        List<TeamAttendanceMemberResponse> members = users.stream()
                .map(user -> {
                    WorkSession session = latestByUser.get(user.getId());
                    return new TeamAttendanceMemberResponse(
                            user.getId(),
                            user.getName(),
                            user.getRoleLevel().name(),
                            session == null ? "NOT_CHECKED_IN" : session.getStatus().name(),
                            session == null ? null : session.getCheckInAt(),
                            session == null ? null : session.getCheckOutAt(),
                            session != null && session.getLastLatitude() != null && session.getLastLongitude() != null
                    );
                })
                .toList();

        return new TeamAttendanceTodayResponse(teamId, team.getName(), LocalDate.now(), members);
    }

    public List<WorkPolicyResponse> listWorkPolicies() {
        return workPolicyRepository.findAll().stream()
                .map(this::toWorkPolicyResponse)
                .toList();
    }

    public List<PermissionResponse> listPermissions() {
        List<User> allUsers = userRepository.findAll();
        Map<RoleLevel, Integer> countByRole = new HashMap<>();
        for (RoleLevel roleLevel : RoleLevel.values()) {
            countByRole.put(roleLevel, 0);
        }
        for (User user : allUsers) {
            countByRole.computeIfPresent(user.getRoleLevel(), (k, v) -> v + 1);
        }

        return Arrays.stream(RoleLevel.values())
                .map(role -> new PermissionResponse(
                        permissionId(role),
                        role.name(),
                        roleDescription(role),
                        countByRole.getOrDefault(role, 0),
                        "2024-01-01"
                ))
                .toList();
    }

    public List<PermissionMemberResponse> permissionMembers(Long permissionId) {
        RoleLevel roleLevel = roleByPermissionId(permissionId);
        return userRepository.findAll().stream()
                .filter(user -> user.getRoleLevel() == roleLevel)
                .map(user -> new PermissionMemberResponse(
                        user.getId(),
                        user.getName(),
                        user.getTeam() == null ? null : user.getTeam().getName()
                ))
                .toList();
    }

    private AttendanceHistoryItemResponse toAttendanceHistoryItem(WorkSession session) {
        boolean late = session.getCheckInAt() != null && session.getCheckInAt().toLocalTime().isAfter(LocalTime.of(9, 0));
        return new AttendanceHistoryItemResponse(
                session.getId(),
                session.getCheckInAt() == null ? null : session.getCheckInAt().toLocalDate(),
                session.getStatus().name(),
                session.getCheckInAt(),
                session.getCheckOutAt(),
                late
        );
    }

    private WorkPolicyResponse toWorkPolicyResponse(WorkPolicy policy) {
        return new WorkPolicyResponse(
                policy.getId(),
                policy.getTeam().getId(),
                policy.getTeam().getName(),
                policy.getTeam().getBranch() == null ? "" : policy.getTeam().getBranch().getName(),
                policy.getCheckinRadiusM(),
                policy.getCheckoutGraceMinutes(),
                "10:00",
                "16:00",
                true
        );
    }

    private Map<Long, WorkSession> latestByUser(List<WorkSession> sessions) {
        Map<Long, WorkSession> latestByUser = new HashMap<>();
        for (WorkSession session : sessions) {
            latestByUser.merge(
                    session.getUser().getId(),
                    session,
                    (oldValue, newValue) -> compareMoment(newValue).isAfter(compareMoment(oldValue)) ? newValue : oldValue
            );
        }
        return latestByUser;
    }

    private LocalDateTime compareMoment(WorkSession session) {
        return List.of(session.getCheckInAt(), session.getCheckOutAt(), session.getOutsideSince())
                .stream()
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(LocalDateTime.MIN);
    }

    private long permissionId(RoleLevel roleLevel) {
        return roleLevel.ordinal() + 1L;
    }

    private RoleLevel roleByPermissionId(Long permissionId) {
        int idx = Math.toIntExact(permissionId - 1);
        RoleLevel[] values = RoleLevel.values();
        if (idx < 0 || idx >= values.length) {
            throw new BusinessException("permission not found");
        }
        return values[idx];
    }

    private String roleDescription(RoleLevel roleLevel) {
        return switch (roleLevel) {
            case DEPARTMENT_HEAD -> "최상위 관리자 권한";
            case TEAM_LEAD -> "팀 관리 및 조회 권한";
            case MANAGER -> "팀/근태 관리 권한";
            case TEAM_MEMBER -> "기본 출퇴근 권한";
        };
    }
}
