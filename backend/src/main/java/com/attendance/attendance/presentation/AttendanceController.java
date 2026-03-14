package com.attendance.attendance.presentation;

import com.attendance.attendance.application.AttendanceQueryService;
import com.attendance.attendance.application.AttendanceTrackingService;
import com.attendance.attendance.presentation.dto.LocationUpdateRequest;
import com.attendance.attendance.presentation.dto.LocationUpdateResponse;
import com.attendance.attendance.presentation.dto.WorkSessionResponse;
import com.attendance.shared.security.UserSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceTrackingService attendanceTrackingService;
    private final AttendanceQueryService attendanceQueryService;

    @PostMapping("/me/location")
    public ResponseEntity<LocationUpdateResponse> updateMyLocation(
            @AuthenticationPrincipal UserSession userSession,
            @Valid @RequestBody LocationUpdateRequest request
    ) {
        LocalDateTime observedAt = request.observedAt() == null ? LocalDateTime.now() : request.observedAt();
        AttendanceTrackingService.TrackingResult result = attendanceTrackingService.processMyLocation(
                resolveLoginId(userSession),
                request.latitude(),
                request.longitude(),
                observedAt
        );
        return ResponseEntity.ok(new LocationUpdateResponse(result.state(), result.message(), result.sessionId(), result.distanceM()));
    }

    @GetMapping("/me/sessions")
    public ResponseEntity<Page<WorkSessionResponse>> mySessions(
            @AuthenticationPrincipal UserSession userSession,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<WorkSessionResponse> mapped = attendanceQueryService.mySessions(resolveUserId(userSession), pageable)
                .map(WorkSessionResponse::from);
        return ResponseEntity.ok(new PageImpl<>(mapped.getContent(), pageable, mapped.getTotalElements()));
    }

    @GetMapping("/visible-sessions")
    public ResponseEntity<Page<WorkSessionResponse>> visibleSessions(
            @AuthenticationPrincipal UserSession userSession,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<WorkSessionResponse> mapped = attendanceQueryService
                .visibleSessionsByLoginId(resolveLoginId(userSession), from, to, pageable)
                .map(WorkSessionResponse::from);
        return ResponseEntity.ok(new PageImpl<>(mapped.getContent(), pageable, mapped.getTotalElements()));
    }

    private String resolveLoginId(UserSession userSession) {
        if (userSession != null) {
            return userSession.getLoginId();
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    private Long resolveUserId(UserSession userSession) {
        if (userSession != null) {
            return userSession.getId();
        }
        return 0L;
    }
}
