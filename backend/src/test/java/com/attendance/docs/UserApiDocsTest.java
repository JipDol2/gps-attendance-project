package com.attendance.docs;

import com.attendance.attendance.domain.WorkPolicy;
import com.attendance.organization.domain.Branch;
import com.attendance.organization.domain.RoleLevel;
import com.attendance.organization.domain.Team;
import com.attendance.shared.security.CustomUserDetailsService;
import com.attendance.shared.security.JwtAuthenticationFilter;
import com.attendance.user.application.AuthService;
import com.attendance.user.application.UserService;
import com.attendance.user.domain.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = com.attendance.user.presentation.UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
class UserApiDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void register() throws Exception {
        User user = sampleUser("hong1234", RoleLevel.TEAM_MEMBER);
        given(userService.register(any(), any(), any(), any(), any())).willReturn(user);

        Map<String, Object> request = Map.of(
                "loginId", "hong1234",
                "password", "password123!",
                "email", "hong@test.com",
                "name", "Hong",
                "teamId", 10
        );

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(MockMvcRestDocumentation.document("users-register",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())));
    }

    @Test
    void login() throws Exception {
        User user = sampleUser("hong1234", RoleLevel.TEAM_LEAD);
        AuthService.AuthTokens tokens = new AuthService.AuthTokens(user, "access-token-example", "refresh-token-example");
        given(authService.login(eq("hong1234"), eq("password123!"))).willReturn(tokens);

        Map<String, Object> request = Map.of(
                "loginId", "hong1234",
                "password", "password123!"
        );

        mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(MockMvcRestDocumentation.document("users-login",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())));
    }

    @Test
    void refresh() throws Exception {
        User user = sampleUser("hong1234", RoleLevel.TEAM_LEAD);
        AuthService.AuthTokens tokens = new AuthService.AuthTokens(user, "new-access-token", "new-refresh-token");
        given(authService.refresh("refresh-token-example")).willReturn(tokens);

        Map<String, Object> request = Map.of(
                "refreshToken", "refresh-token-example"
        );

        mockMvc.perform(post("/api/v1/users/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(MockMvcRestDocumentation.document("users-refresh",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())));
    }

    @Test
    @WithMockUser(username = "hong1234")
    void updateMyOrganization() throws Exception {
        User user = sampleUser("hong1234", RoleLevel.MANAGER);
        given(userService.updateMyTeam(eq("hong1234"), eq(RoleLevel.MANAGER), eq(10L))).willReturn(user);

        Map<String, Object> request = Map.of(
                "roleLevel", "MANAGER",
                "teamId", 10
        );

        mockMvc.perform(patch("/api/v1/users/me/team")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(MockMvcRestDocumentation.document("users-update-my-organization",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())));
    }

    private User sampleUser(String loginId, RoleLevel roleLevel) {
        Branch branch = new Branch("Euljiro", 37.5, 127.0);
        ReflectionTestUtils.setField(branch, "id", 1L);
        Team team = new Team("Platform", null, branch);
        ReflectionTestUtils.setField(team, "id", 10L);
        WorkPolicy policy = new WorkPolicy("HQ", 200, 300, 10, team);
        ReflectionTestUtils.setField(policy, "id", 20L);

        User user = new User(loginId, "encoded-password", loginId + "@test.com", "Hong", roleLevel, team, policy);
        user.grantHrAuthority();
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.of(2026, 2, 17, 9, 0));
        ReflectionTestUtils.setField(user, "lastLoginAt", LocalDateTime.of(2026, 2, 17, 9, 5));
        return user;
    }
}
