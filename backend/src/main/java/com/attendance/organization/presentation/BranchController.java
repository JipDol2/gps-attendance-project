package com.attendance.organization.presentation;

import com.attendance.organization.application.OrganizationCommandService;
import com.attendance.organization.domain.Branch;
import com.attendance.organization.presentation.dto.BranchResponse;
import com.attendance.organization.presentation.dto.BranchSummaryResponse;
import com.attendance.organization.presentation.dto.CreateBranchRequest;
import com.attendance.organization.presentation.dto.UpdateBranchRequest;
import com.attendance.shared.security.UserSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
public class BranchController {

    private final OrganizationCommandService organizationCommandService;

    @GetMapping
    public ResponseEntity<List<BranchSummaryResponse>> list() {
        List<BranchSummaryResponse> branches = organizationCommandService.listBranches()
                .stream()
                .map(BranchSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(branches);
    }

    @PostMapping
    public ResponseEntity<BranchResponse> create(
            @AuthenticationPrincipal UserSession userSession,
            @Valid @RequestBody CreateBranchRequest request
    ) {
        Branch branch = organizationCommandService.createBranch(
                resolveLoginId(userSession),
                request.name(),
                request.latitude(),
                request.longitude()
        );
        return ResponseEntity.ok(BranchResponse.from(branch));
    }

    @PatchMapping("/{branchId}")
    public ResponseEntity<BranchResponse> update(
            @AuthenticationPrincipal UserSession userSession,
            @PathVariable Long branchId,
            @Valid @RequestBody UpdateBranchRequest request
    ) {
        Branch branch = organizationCommandService.updateBranch(
                resolveLoginId(userSession),
                branchId,
                request.name(),
                request.latitude(),
                request.longitude()
        );
        return ResponseEntity.ok(BranchResponse.from(branch));
    }

    @DeleteMapping("/{branchId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserSession userSession,
            @PathVariable Long branchId
    ) {
        organizationCommandService.deleteBranch(resolveLoginId(userSession), branchId);
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
