package com.attendance.mobile.presentation.dto;

import java.time.LocalDateTime;

public record TeamAttendanceMemberResponse(
        Long userId,
        String userName,
        String position,
        String status,
        LocalDateTime checkInAt,
        LocalDateTime checkOutAt,
        boolean locationKnown
) {
}

