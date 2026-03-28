package com.attendance.mobile.presentation.dto;

public record PermissionMemberResponse(
        Long userId,
        String userName,
        String teamName
) {
}

