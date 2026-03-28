package com.attendance.mobile.presentation.dto;

import java.time.LocalDate;

public record DashboardSummaryResponse(
        Long teamId,
        String teamName,
        LocalDate workDate,
        int totalMembers,
        int checkedInMembers,
        int notCheckedInMembers,
        String myStatus
) {
}
