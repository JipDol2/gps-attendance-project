package com.attendance.organization.presentation.dto;

import com.attendance.organization.domain.Branch;

public record BranchResponse(
        Long id,
        String name,
        double latitude,
        double longitude
) {
    public static BranchResponse from(Branch branch) {
        return new BranchResponse(branch.getId(), branch.getName(), branch.getLatitude(), branch.getLongitude());
    }
}
