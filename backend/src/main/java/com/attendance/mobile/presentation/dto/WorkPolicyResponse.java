package com.attendance.mobile.presentation.dto;

public record WorkPolicyResponse(
        Long policyId,
        Long teamId,
        String teamName,
        String workAddress,
        int allowedRadiusM,
        int graceMinutes,
        String coreTimeStart,
        String coreTimeEnd,
        boolean enabled
) {
}

