package com.attendance.organization.presentation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateWorkPolicyRequest(
        @NotNull Long teamId,
        @NotBlank String name,
        @Min(1) @Max(10000) Integer checkinRadiusM,
        @Min(1) @Max(10000) Integer checkoutRadiusM,
        @Min(1) @Max(1440) Integer checkoutGraceMinutes
) {
}
