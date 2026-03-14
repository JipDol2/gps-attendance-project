package com.attendance.organization.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTeamRequest(
        Long parentTeamId,
        @NotBlank String name,
        @NotNull Long branchId
) {
}
