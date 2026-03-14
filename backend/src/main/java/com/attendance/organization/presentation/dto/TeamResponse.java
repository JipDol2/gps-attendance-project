package com.attendance.organization.presentation.dto;

import com.attendance.organization.domain.Team;

public record TeamResponse(
        Long id,
        String name,
        Long parentTeamId,
        Long branchId,
        String branchName
) {
    public static TeamResponse from(Team team) {
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getParentTeam() == null ? null : team.getParentTeam().getId(),
                team.getBranch() == null ? null : team.getBranch().getId(),
                team.getBranch() == null ? null : team.getBranch().getName()
        );
    }
}
