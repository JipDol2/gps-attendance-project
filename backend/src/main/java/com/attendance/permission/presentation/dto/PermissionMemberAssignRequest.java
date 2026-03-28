package com.attendance.permission.presentation.dto;

import jakarta.validation.constraints.NotNull;

public record PermissionMemberAssignRequest(
        @NotNull Long userId
) {
}

