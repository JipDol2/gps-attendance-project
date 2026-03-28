package com.attendance.mobile.presentation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AttendanceHistoryItemResponse(
        Long sessionId,
        LocalDate workDate,
        String status,
        LocalDateTime checkInAt,
        LocalDateTime checkOutAt,
        boolean late
) {
}

