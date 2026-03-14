package com.attendance.docs;

import com.attendance.attendance.application.AttendanceQueryService;
import com.attendance.attendance.application.AttendanceTrackingService;
import com.attendance.attendance.application.TrackingState;
import com.attendance.attendance.domain.WorkPolicy;
import com.attendance.attendance.domain.WorkSession;
import com.attendance.organization.domain.Branch;
import com.attendance.organization.domain.RoleLevel;
import com.attendance.organization.domain.Team;
import com.attendance.shared.security.CustomUserDetailsService;
import com.attendance.shared.security.JwtAuthenticationFilter;
import com.attendance.user.domain.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = com.attendance.attendance.presentation.AttendanceController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
class AttendanceApiDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AttendanceTrackingService attendanceTrackingService;

    @MockBean
    private AttendanceQueryService attendanceQueryService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(username = "hong1234")
    void updateMyLocation() throws Exception {
        AttendanceTrackingService.TrackingResult result = new AttendanceTrackingService.TrackingResult(
                TrackingState.CHECKED_IN_STARTED,
                "checked in",
                99L,
                23.4
        );
        given(attendanceTrackingService.processMyLocation(eq("hong1234"), eq(37.5001), eq(127.0001), any(LocalDateTime.class)))
                .willReturn(result);

        Map<String, Object> request = Map.of(
                "latitude", 37.5001,
                "longitude", 127.0001,
                "observedAt", "2026-02-17T09:00:00"
        );

        mockMvc.perform(post("/api/v1/attendance/me/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(MockMvcRestDocumentation.document("attendance-update-my-location",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())));
    }

    @Test
    @WithMockUser(username = "hong1234")
    void mySessions() throws Exception {
        WorkSession session = sampleSession(1L, "Hong", LocalDateTime.of(2026, 2, 17, 9, 0));
        given(attendanceQueryService.mySessions(anyLong(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(session)));

        mockMvc.perform(get("/api/v1/attendance/me/sessions"))
                .andExpect(status().isOk())
                .andDo(MockMvcRestDocumentation.document("attendance-my-sessions",
                        preprocessResponse(prettyPrint())));
    }

    @Test
    @WithMockUser(username = "lead1")
    void visibleSessions() throws Exception {
        WorkSession session = sampleSession(2L, "Kim", LocalDateTime.of(2026, 2, 17, 10, 0));
        given(attendanceQueryService.visibleSessionsByLoginId(
                eq("lead1"),
                eq(LocalDateTime.of(2026, 2, 17, 0, 0)),
                eq(LocalDateTime.of(2026, 2, 17, 23, 59, 59)),
                any(Pageable.class)
        )).willReturn(new PageImpl<>(List.of(session)));

        mockMvc.perform(get("/api/v1/attendance/visible-sessions")
                        .param("from", "2026-02-17T00:00:00")
                        .param("to", "2026-02-17T23:59:59"))
                .andExpect(status().isOk())
                .andDo(MockMvcRestDocumentation.document("attendance-visible-sessions",
                        preprocessResponse(prettyPrint())));
    }

    private WorkSession sampleSession(Long userId, String userName, LocalDateTime checkInAt) {
        Branch branch = new Branch("Euljiro", 37.5, 127.0);
        ReflectionTestUtils.setField(branch, "id", 1L);
        Team team = new Team("Platform", null, branch);
        ReflectionTestUtils.setField(team, "id", 10L);

        WorkPolicy policy = new WorkPolicy("HQ", 200, 300, 10, team);
        ReflectionTestUtils.setField(policy, "id", 20L);

        User user = new User("login-" + userId, "encoded", userId + "@test.com", userName, RoleLevel.TEAM_MEMBER, team, policy);
        ReflectionTestUtils.setField(user, "id", userId);

        WorkSession session = new WorkSession(user, policy, checkInAt, 37.5, 127.0);
        ReflectionTestUtils.setField(session, "id", 1000L + userId);
        return session;
    }
}
