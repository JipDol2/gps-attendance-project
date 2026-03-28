package com.attendance.mobile.presentation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WorkPolicyUpsertRequest(
        @NotNull Long teamId,
        @NotBlank String workAddress,
        @Min(1) int allowedRadiusM,
        @Min(1) int graceMinutes,
        @NotBlank String coreTimeStart,
        @NotBlank String coreTimeEnd,
        boolean enabled
) {
}

