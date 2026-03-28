package com.attendance.mobile.presentation;

import com.attendance.mobile.application.MobileQueryService;
import com.attendance.mobile.presentation.dto.PermissionMemberResponse;
import com.attendance.mobile.presentation.dto.PermissionResponse;
import com.attendance.mobile.presentation.dto.PermissionUpsertRequest;
import com.attendance.mobile.presentation.dto.WorkPolicyResponse;
import com.attendance.mobile.presentation.dto.WorkPolicyUpsertRequest;
import com.attendance.organization.application.OrganizationCommandService;
import com.attendance.permission.application.PermissionService;
import com.attendance.permission.presentation.dto.PermissionMemberAssignRequest;
import com.attendance.shared.security.UserSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AdminMobileController {

    private final MobileQueryService mobileQueryService;
    private final OrganizationCommandService organizationCommandService;
    private final PermissionService permissionService;

    @GetMapping("/work-policies")
    public ResponseEntity<List<WorkPolicyResponse>> listWorkPolicies() {
        return ResponseEntity.ok(mobileQueryService.listWorkPolicies());
    }

    @PutMapping("/work-policies/{policyId}")
    public ResponseEntity<WorkPolicyResponse> updatePolicy(
            @AuthenticationPrincipal UserSession userSession,
            @PathVariable Long policyId,
            @Valid @RequestBody WorkPolicyUpsertRequest request
    ) {
        var updated = organizationCommandService.updateWorkPolicy(
                resolveLoginId(userSession),
                policyId,
                request.teamId(),
                "Mobile Policy " + request.teamId(),
                request.allowedRadiusM(),
                request.allowedRadiusM(),
                request.graceMinutes()
        );

        return ResponseEntity.ok(
                new WorkPolicyResponse(
                        updated.getId(),
                        updated.getTeam().getId(),
                        updated.getTeam().getName(),
                        updated.getTeam().getBranch() == null ? "" : updated.getTeam().getBranch().getName(),
                        updated.getCheckinRadiusM(),
                        updated.getCheckoutGraceMinutes(),
                        request.coreTimeStart(),
                        request.coreTimeEnd(),
                        request.enabled()
                )
        );
    }

    @GetMapping("/permissions")
    public ResponseEntity<List<PermissionResponse>> listPermissions() {
        return ResponseEntity.ok(permissionService.listPermissions());
    }

    @GetMapping("/permissions/{permissionId}/members")
    public ResponseEntity<List<PermissionMemberResponse>> permissionMembers(@PathVariable Long permissionId) {
        return ResponseEntity.ok(permissionService.permissionMembers(permissionId));
    }

    @PostMapping("/permissions")
    public ResponseEntity<PermissionResponse> createPermission(
            @AuthenticationPrincipal UserSession userSession,
            @Valid @RequestBody PermissionUpsertRequest request
    ) {
        return ResponseEntity.ok(permissionService.createPermission(resolveLoginId(userSession), request));
    }

    @PutMapping("/permissions/{permissionId}")
    public ResponseEntity<PermissionResponse> updatePermission(
            @AuthenticationPrincipal UserSession userSession,
            @PathVariable Long permissionId,
            @Valid @RequestBody PermissionUpsertRequest request
    ) {
        return ResponseEntity.ok(permissionService.updatePermission(resolveLoginId(userSession), permissionId, request));
    }

    @DeleteMapping("/permissions/{permissionId}")
    public ResponseEntity<Void> deletePermission(
            @AuthenticationPrincipal UserSession userSession,
            @PathVariable Long permissionId
    ) {
        permissionService.deletePermission(resolveLoginId(userSession), permissionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/permissions/{permissionId}/members")
    public ResponseEntity<Void> assignPermissionMember(
            @AuthenticationPrincipal UserSession userSession,
            @PathVariable Long permissionId,
            @Valid @RequestBody PermissionMemberAssignRequest request
    ) {
        permissionService.assignMember(resolveLoginId(userSession), permissionId, request.userId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/permissions/{permissionId}/members/{userId}")
    public ResponseEntity<Void> unassignPermissionMember(
            @AuthenticationPrincipal UserSession userSession,
            @PathVariable Long permissionId,
            @PathVariable Long userId
    ) {
        permissionService.unassignMember(resolveLoginId(userSession), permissionId, userId);
        return ResponseEntity.noContent().build();
    }

    private String resolveLoginId(UserSession userSession) {
        if (userSession != null) {
            return userSession.getLoginId();
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }
}
