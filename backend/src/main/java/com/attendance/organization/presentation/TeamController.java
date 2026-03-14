package com.attendance.organization.presentation;

import com.attendance.attendance.domain.WorkPolicy;
import com.attendance.organization.application.OrganizationCommandService;
import com.attendance.organization.domain.Team;
import com.attendance.organization.presentation.dto.CreateTeamRequest;
import com.attendance.organization.presentation.dto.CreateWorkPolicyRequest;
import com.attendance.organization.presentation.dto.TeamResponse;
import com.attendance.organization.presentation.dto.TeamSummaryResponse;
import com.attendance.organization.presentation.dto.UpdateTeamRequest;
import com.attendance.organization.presentation.dto.UpdateWorkPolicyRequest;
import com.attendance.shared.security.UserSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TeamController {

    private final OrganizationCommandService organizationCommandService;

    @GetMapping("/teams")
    public ResponseEntity<List<TeamSummaryResponse>> list() {
        List<TeamSummaryResponse> teams = organizationCommandService.listTeams()
                .stream()
                .map(TeamSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(teams);
    }

    @PostMapping("/teams")
    public ResponseEntity<TeamResponse> createTeam(
            @AuthenticationPrincipal UserSession userSession,
            @Valid @RequestBody CreateTeamRequest request
    ) {
        Team team = organizationCommandService.createTeam(
                resolveLoginId(userSession),
                request.parentTeamId(),
                request.name(),
                request.branchId()
        );
        return ResponseEntity.ok(TeamResponse.from(team));
    }

    @PatchMapping("/teams/{teamId}")
    public ResponseEntity<TeamResponse> updateTeam(
            @AuthenticationPrincipal UserSession userSession,
            @PathVariable Long teamId,
            @Valid @RequestBody UpdateTeamRequest request
    ) {
        Team team = organizationCommandService.updateTeam(
                resolveLoginId(userSession),
                teamId,
                request.name(),
                request.parentTeamId(),
                request.branchId()
        );
        return ResponseEntity.ok(TeamResponse.from(team));
    }

    @PostMapping("/teams/work-policies")
    public ResponseEntity<Map<String, Object>> createWorkPolicy(
            @AuthenticationPrincipal UserSession userSession,
            @Valid @RequestBody CreateWorkPolicyRequest request
    ) {
        WorkPolicy policy = organizationCommandService.createWorkPolicy(
                resolveLoginId(userSession),
                request.teamId(),
                request.name(),
                request.checkinRadiusM(),
                request.checkoutRadiusM(),
                request.checkoutGraceMinutes()
        );
        return ResponseEntity.ok(Map.of(
                "id", policy.getId(),
                "name", policy.getName(),
                "teamId", policy.getTeam().getId()
        ));
    }

    @PatchMapping("/teams/work-policies/{policyId}")
    public ResponseEntity<Map<String, Object>> updateWorkPolicy(
            @AuthenticationPrincipal UserSession userSession,
            @PathVariable Long policyId,
            @Valid @RequestBody UpdateWorkPolicyRequest request
    ) {
        WorkPolicy policy = organizationCommandService.updateWorkPolicy(
                resolveLoginId(userSession),
                policyId,
                request.teamId(),
                request.name(),
                request.checkinRadiusM(),
                request.checkoutRadiusM(),
                request.checkoutGraceMinutes()
        );
        return ResponseEntity.ok(Map.of(
                "id", policy.getId(),
                "name", policy.getName(),
                "teamId", policy.getTeam().getId()
        ));
    }

    private String resolveLoginId(UserSession userSession) {
        if (userSession != null) {
            return userSession.getLoginId();
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }
}
