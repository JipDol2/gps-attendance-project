package com.attendance.mobile.presentation.dto;

public record PermissionResponse(
        Long permissionId,
        String name,
        String description,
        int memberCount,
        String createdAt
) {
}

