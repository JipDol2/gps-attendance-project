package com.attendance.organization.application;

import com.attendance.attendance.domain.WorkPolicy;
import com.attendance.attendance.infrastructure.WorkPolicyRepository;
import com.attendance.organization.domain.Branch;
import com.attendance.organization.domain.RoleLevel;
import com.attendance.organization.domain.Team;
import com.attendance.organization.infrastructure.BranchRepository;
import com.attendance.organization.infrastructure.TeamRepository;
import com.attendance.shared.exception.BusinessException;
import com.attendance.user.domain.User;
import com.attendance.user.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationCommandServiceTest {

    @Mock
    private TeamRepository teamRepository;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private WorkPolicyRepository workPolicyRepository;
    @Mock
    private UserRepository userRepository;

    private OrganizationCommandService organizationCommandService;

    @BeforeEach
    void setUp() {
        organizationCommandService = new OrganizationCommandService(teamRepository, branchRepository, workPolicyRepository, userRepository);
    }

    @Test
    void updateTeamChangesNameParentAndBranchWhenActorIsHr() {
        Branch hq = new Branch("HQ", 37.5, 127.0);
        Team root = new Team("Root", null, hq);
        Team team = new Team("Platform", null, hq);
        User hr = new User("hr1", "pw", "hr@test.com", "Hr", RoleLevel.TEAM_MEMBER, team, null);
        hr.grantHrAuthority();

        ReflectionTestUtils.setField(root, "id", 10L);
        ReflectionTestUtils.setField(team, "id", 100L);
        ReflectionTestUtils.setField(hq, "id", 3L);

        when(userRepository.findByLoginId("hr1")).thenReturn(Optional.of(hr));
        when(teamRepository.findById(100L)).thenReturn(Optional.of(team));
        when(teamRepository.findById(10L)).thenReturn(Optional.of(root));
        when(branchRepository.findById(3L)).thenReturn(Optional.of(hq));
        when(teamRepository.existsByParentTeamAndNameAndIdNot(root, "Platform2", 100L)).thenReturn(false);
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Team updated = organizationCommandService.updateTeam("hr1", 100L, "Platform2", 10L, 3L);

        assertThat(updated.getName()).isEqualTo("Platform2");
        assertThat(updated.getParentTeam()).isEqualTo(root);
        assertThat(updated.getBranch()).isEqualTo(hq);
    }

    @Test
    void createTeamDeniedForNonHr() {
        User lead = new User("lead1", "pw", "lead@test.com", "Lead", RoleLevel.TEAM_LEAD, null, null);
        when(userRepository.findByLoginId("lead1")).thenReturn(Optional.of(lead));

        assertThatThrownBy(() -> organizationCommandService.createTeam("lead1", null, "Engineering", 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("permission denied");
    }

    @Test
    void updateTeamRejectsSelfParent() {
        Branch hq = new Branch("HQ", 37.5, 127.0);
        Team team = new Team("Platform", null, hq);
        User hr = new User("hr1", "pw", "hr@test.com", "Hr", RoleLevel.TEAM_MEMBER, team, null);
        hr.grantHrAuthority();
        ReflectionTestUtils.setField(team, "id", 100L);

        when(userRepository.findByLoginId("hr1")).thenReturn(Optional.of(hr));
        when(teamRepository.findById(100L)).thenReturn(Optional.of(team));

        assertThatThrownBy(() -> organizationCommandService.updateTeam("hr1", 100L, "Platform", 100L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("team cannot be parent of itself");
    }

    @Test
    void updateTeamRejectsCycle() {
        Branch hq = new Branch("HQ", 37.5, 127.0);
        Team root = new Team("Root", null, hq);
        Team parent = new Team("Parent", root, hq);
        Team child = new Team("Child", parent, hq);
        User hr = new User("hr1", "pw", "hr@test.com", "Hr", RoleLevel.TEAM_MEMBER, parent, null);
        hr.grantHrAuthority();

        ReflectionTestUtils.setField(root, "id", 1L);
        ReflectionTestUtils.setField(parent, "id", 2L);
        ReflectionTestUtils.setField(child, "id", 3L);

        when(userRepository.findByLoginId("hr1")).thenReturn(Optional.of(hr));
        when(teamRepository.findById(2L)).thenReturn(Optional.of(parent));
        when(teamRepository.findById(3L)).thenReturn(Optional.of(child));

        assertThatThrownBy(() -> organizationCommandService.updateTeam("hr1", 2L, "Parent", 3L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("cyclic team hierarchy is not allowed");
    }

    @Test
    void updateWorkPolicyAllowedForHr() {
        Branch hq = new Branch("HQ", 37.5, 127.0);
        Team team = new Team("Platform", null, hq);
        WorkPolicy policy = new WorkPolicy("Old", 200, 300, 10, team);
        User hr = new User("hr1", "pw", "hr@test.com", "Hr", RoleLevel.TEAM_MEMBER, team, null);
        hr.grantHrAuthority();

        ReflectionTestUtils.setField(team, "id", 10L);
        ReflectionTestUtils.setField(policy, "id", 1L);

        when(userRepository.findByLoginId("hr1")).thenReturn(Optional.of(hr));
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(workPolicyRepository.findById(1L)).thenReturn(Optional.of(policy));
        when(workPolicyRepository.save(any(WorkPolicy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkPolicy updated = organizationCommandService.updateWorkPolicy(
                "hr1", 1L, 10L, "New", 250, 350, 15
        );

        assertThat(updated.getName()).isEqualTo("New");
        assertThat(updated.getTeam()).isEqualTo(team);
    }

    @Test
    void updateWorkPolicyDeniedForTeamLeadWithoutHr() {
        Branch hq = new Branch("HQ", 37.5, 127.0);
        Team team = new Team("Platform", null, hq);
        User lead = new User("lead1", "pw", "lead@test.com", "Lead", RoleLevel.TEAM_LEAD, team, null);

        when(userRepository.findByLoginId("lead1")).thenReturn(Optional.of(lead));

        assertThatThrownBy(() -> organizationCommandService.updateWorkPolicy(
                "lead1", 1L, 10L, "New", 250, 350, 15
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("permission denied");
    }

    @Test
    void deleteBranchBlockedWhenTeamsAssigned() {
        Branch branch = new Branch("HQ", 37.5, 127.0);
        User hr = new User("hr1", "pw", "hr@test.com", "Hr", RoleLevel.TEAM_MEMBER, null, null);
        hr.grantHrAuthority();
        ReflectionTestUtils.setField(branch, "id", 10L);

        when(userRepository.findByLoginId("hr1")).thenReturn(Optional.of(hr));
        when(branchRepository.findById(10L)).thenReturn(Optional.of(branch));
        when(teamRepository.existsByBranchId(10L)).thenReturn(true);

        assertThatThrownBy(() -> organizationCommandService.deleteBranch("hr1", 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("cannot delete branch with assigned teams");

        verify(branchRepository, never()).delete(any(Branch.class));
    }

    @Test
    void deleteBranchSucceedsWhenNoAssignedTeams() {
        Branch branch = new Branch("HQ", 37.5, 127.0);
        User hr = new User("hr1", "pw", "hr@test.com", "Hr", RoleLevel.TEAM_MEMBER, null, null);
        hr.grantHrAuthority();
        ReflectionTestUtils.setField(branch, "id", 11L);

        when(userRepository.findByLoginId("hr1")).thenReturn(Optional.of(hr));
        when(branchRepository.findById(11L)).thenReturn(Optional.of(branch));
        when(teamRepository.existsByBranchId(11L)).thenReturn(false);

        organizationCommandService.deleteBranch("hr1", 11L);

        verify(branchRepository).delete(branch);
    }
}
