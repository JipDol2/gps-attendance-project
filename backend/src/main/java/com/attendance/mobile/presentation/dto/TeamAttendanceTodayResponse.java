package com.attendance.mobile.presentation.dto;

import java.time.LocalDate;
import java.util.List;

public record TeamAttendanceTodayResponse(
        Long teamId,
        String teamName,
        LocalDate workDate,
        List<TeamAttendanceMemberResponse> members
) {
}

