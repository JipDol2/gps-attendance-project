package com.attendance.organization.presentation.dto;

import com.attendance.organization.domain.Team;

public record TeamSummaryResponse(
        Long id,
        String name,
        Long branchId,
        String branchName
) {
    public static TeamSummaryResponse from(Team team) {
        return new TeamSummaryResponse(
                team.getId(),
                team.getName(),
                team.getBranch() == null ? null : team.getBranch().getId(),
                team.getBranch() == null ? null : team.getBranch().getName()
        );
    }
}
