package com.attendance.organization.presentation.dto;

import com.attendance.organization.domain.Branch;

public record BranchSummaryResponse(
        Long id,
        String name,
        double latitude,
        double longitude
) {
    public static BranchSummaryResponse from(Branch branch) {
        return new BranchSummaryResponse(branch.getId(), branch.getName(), branch.getLatitude(), branch.getLongitude());
    }
}
