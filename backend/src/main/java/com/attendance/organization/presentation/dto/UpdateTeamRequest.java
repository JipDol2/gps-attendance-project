package com.attendance.organization.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTeamRequest(
        @NotBlank @Size(max = 100) String name,
        Long parentTeamId,
        @NotNull Long branchId
) {
}
