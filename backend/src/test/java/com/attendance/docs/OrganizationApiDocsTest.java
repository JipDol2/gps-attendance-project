package com.attendance.docs;

import com.attendance.attendance.domain.WorkPolicy;
import com.attendance.organization.application.OrganizationCommandService;
import com.attendance.organization.domain.Branch;
import com.attendance.organization.domain.Team;
import com.attendance.shared.security.CustomUserDetailsService;
import com.attendance.shared.security.JwtAuthenticationFilter;
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

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = com.attendance.organization.presentation.TeamController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
class OrganizationApiDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrganizationCommandService organizationCommandService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(username = "hr1")
    void createTeam() throws Exception {
        Branch branch = new Branch("Euljiro", 37.5, 127.0);
        ReflectionTestUtils.setField(branch, "id", 3L);
        Team team = new Team("Engineering", null, branch);
        ReflectionTestUtils.setField(team, "id", 1L);
        given(organizationCommandService.createTeam("hr1", null, "Engineering", 3L)).willReturn(team);

        Map<String, Object> request = Map.of(
                "name", "Engineering",
                "branchId", 3
        );

        mockMvc.perform(post("/api/v1/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(MockMvcRestDocumentation.document("organization-create-team",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())));
    }

    @Test
    @WithMockUser(username = "hr1")
    void updateTeam() throws Exception {
        Branch branch = new Branch("Euljiro", 37.5, 127.0);
        ReflectionTestUtils.setField(branch, "id", 3L);
        Team parentTeam = new Team("Engineering", null, branch);
        ReflectionTestUtils.setField(parentTeam, "id", 1L);
        Team team = new Team("Platform", parentTeam, branch);
        ReflectionTestUtils.setField(team, "id", 10L);

        given(organizationCommandService.updateTeam("hr1", 10L, "Platform", 1L, 3L)).willReturn(team);

        Map<String, Object> request = Map.of(
                "name", "Platform",
                "parentTeamId", 1,
                "branchId", 3
        );

        mockMvc.perform(patch("/api/v1/teams/{teamId}", 10)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(MockMvcRestDocumentation.document("organization-update-team",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())));
    }

    @Test
    @WithMockUser(username = "hr1")
    void createWorkPolicy() throws Exception {
        Branch branch = new Branch("Euljiro", 37.5, 127.0);
        Team team = new Team("Platform", null, branch);
        ReflectionTestUtils.setField(team, "id", 10L);
        WorkPolicy policy = new WorkPolicy("HQ Policy", 200, 300, 10, team);
        ReflectionTestUtils.setField(policy, "id", 1L);

        given(organizationCommandService.createWorkPolicy(eq("hr1"), anyLong(), anyString(), anyInt(), anyInt(), anyInt()))
                .willReturn(policy);

        Map<String, Object> request = Map.of(
                "teamId", 10,
                "name", "HQ Policy",
                "checkinRadiusM", 200,
                "checkoutRadiusM", 300,
                "checkoutGraceMinutes", 10
        );

        mockMvc.perform(post("/api/v1/teams/work-policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(MockMvcRestDocumentation.document("organization-create-work-policy",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())));
    }

    @Test
    @WithMockUser(username = "lead1")
    void updateWorkPolicy() throws Exception {
        Branch branch = new Branch("Euljiro", 37.5, 127.0);
        Team team = new Team("Platform", null, branch);
        ReflectionTestUtils.setField(team, "id", 10L);
        WorkPolicy policy = new WorkPolicy("Updated HQ Policy", 250, 350, 15, team);
        ReflectionTestUtils.setField(policy, "id", 1L);

        given(organizationCommandService.updateWorkPolicy(
                eq("lead1"), eq(1L), anyLong(), anyString(), anyInt(), anyInt(), anyInt()
        )).willReturn(policy);

        Map<String, Object> request = Map.of(
                "teamId", 10,
                "name", "Updated HQ Policy",
                "checkinRadiusM", 250,
                "checkoutRadiusM", 350,
                "checkoutGraceMinutes", 15
        );

        mockMvc.perform(patch("/api/v1/teams/work-policies/{policyId}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(MockMvcRestDocumentation.document("organization-update-work-policy",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())));
    }
}
