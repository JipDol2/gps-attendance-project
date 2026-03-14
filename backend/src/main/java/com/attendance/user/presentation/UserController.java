package com.attendance.user.presentation;

import com.attendance.shared.security.UserSession;
import com.attendance.user.application.AuthService;
import com.attendance.user.application.UserService;
import com.attendance.user.domain.User;
import com.attendance.user.presentation.dto.LoginRequest;
import com.attendance.user.presentation.dto.RefreshTokenRequest;
import com.attendance.user.presentation.dto.RegisterUserRequest;
import com.attendance.user.presentation.dto.TokenResponse;
import com.attendance.user.presentation.dto.UpdateUserOrganizationRequest;
import com.attendance.user.presentation.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        User user = userService.register(
                request.loginId(),
                request.password(),
                request.email(),
                request.name(),
                request.teamId()
        );
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.AuthTokens tokens = authService.login(request.loginId(), request.password());
        return ResponseEntity.ok(new TokenResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                "Bearer",
                UserResponse.from(tokens.user())
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthService.AuthTokens tokens = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(new TokenResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                "Bearer",
                UserResponse.from(tokens.user())
        ));
    }

    @PatchMapping("/me/team")
    public ResponseEntity<UserResponse> updateTeam(
            @AuthenticationPrincipal UserSession userSession,
            @Valid @RequestBody UpdateUserOrganizationRequest request
    ) {
        User user = userService.updateMyTeam(resolveLoginId(userSession), request.roleLevel(), request.teamId());
        return ResponseEntity.ok(UserResponse.from(user));
    }

    private String resolveLoginId(UserSession userSession) {
        if (userSession != null) {
            return userSession.getLoginId();
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }
}
