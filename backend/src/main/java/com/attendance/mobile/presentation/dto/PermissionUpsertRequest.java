package com.attendance.mobile.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record PermissionUpsertRequest(
        @NotBlank String name,
        String description
) {
}
