package com.attendance.user.application;

import com.attendance.attendance.domain.WorkPolicy;
import com.attendance.attendance.infrastructure.WorkPolicyRepository;
import com.attendance.organization.domain.Branch;
import com.attendance.organization.domain.RoleLevel;
import com.attendance.organization.domain.Team;
import com.attendance.organization.infrastructure.TeamRepository;
import com.attendance.shared.exception.BusinessException;
import com.attendance.user.domain.User;
import com.attendance.user.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private WorkPolicyRepository workPolicyRepository;

    private UserService userService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, teamRepository, workPolicyRepository, passwordEncoder);
    }

    @Test
    void registerCreatesMemberWithEncodedPasswordAndTeamPolicy() {
        Branch branch = new Branch("Euljiro", 37.5, 127.0);
        Team team = new Team("Platform", null, branch);
        ReflectionTestUtils.setField(team, "id", 10L);
        WorkPolicy policy = new WorkPolicy("HQ", 200, 300, 10, team);

        when(userRepository.existsByLoginId("hong123")).thenReturn(false);
        when(userRepository.existsByEmail("hong@test.com")).thenReturn(false);
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(workPolicyRepository.findFirstByTeamIdOrderByIdDesc(10L)).thenReturn(Optional.of(policy));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });

        User user = userService.register("hong123", "password123!", "hong@test.com", "Hong", 10L);

        assertThat(user.getId()).isEqualTo(1L);
        assertThat(passwordEncoder.matches("password123!", user.getPasswordHash())).isTrue();
        assertThat(user.getRoleLevel()).isEqualTo(RoleLevel.TEAM_MEMBER);
        assertThat(user.getTeam()).isEqualTo(team);
        assertThat(user.getWorkPolicy()).isEqualTo(policy);
    }

    @Test
    void registerSucceedsWhenTeamPolicyMissing() {
        Branch branch = new Branch("Euljiro", 37.5, 127.0);
        Team team = new Team("Platform", null, branch);
        ReflectionTestUtils.setField(team, "id", 10L);

        when(userRepository.existsByLoginId("u1")).thenReturn(false);
        when(userRepository.existsByEmail("u1@test.com")).thenReturn(false);
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(workPolicyRepository.findFirstByTeamIdOrderByIdDesc(10L)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 2L);
            return saved;
        });

        User user = userService.register("u1", "password123!", "u1@test.com", "U1", 10L);

        assertThat(user.getWorkPolicy()).isNull();
        assertThat(user.getTeam()).isEqualTo(team);
    }

    @Test
    void loginFailsWhenPasswordDoesNotMatch() {
        User saved = new User("hong123", passwordEncoder.encode("password123!"), "hong@test.com", "Hong", RoleLevel.TEAM_MEMBER, null, null);
        when(userRepository.findByLoginIdWithRelations("hong123")).thenReturn(Optional.of(saved));

        assertThatThrownBy(() -> userService.login("hong123", "wrong"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("invalid login id or password");
    }

    @Test
    void updateMyTeamUpdatesRoleAndTeam() {
        Branch branch = new Branch("Euljiro", 37.5, 127.0);
        Team root = new Team("Engineering", null, branch);
        Team oldTeam = new Team("Platform", root, branch);
        Team newTeam = new Team("App", root, branch);
        User user = new User("u1", passwordEncoder.encode("password123!"), "u1@test.com", "U1", RoleLevel.TEAM_MEMBER, oldTeam, null);
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(newTeam, "id", 200L);

        when(userRepository.findByLoginIdWithRelations("u1")).thenReturn(Optional.of(user));
        when(teamRepository.findById(200L)).thenReturn(Optional.of(newTeam));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userService.updateMyTeam("u1", RoleLevel.TEAM_MEMBER, 200L);

        assertThat(updated.getRoleLevel()).isEqualTo(RoleLevel.TEAM_MEMBER);
        assertThat(updated.getTeam()).isEqualTo(newTeam);
    }

    @Test
    void updateMyTeamFailsWhenTeamNotFound() {
        Branch branch = new Branch("Euljiro", 37.5, 127.0);
        Team root = new Team("Engineering", null, branch);
        Team oldTeam = new Team("Platform", root, branch);
        User user = new User("u1", passwordEncoder.encode("password123!"), "u1@test.com", "U1", RoleLevel.TEAM_MEMBER, oldTeam, null);
        ReflectionTestUtils.setField(user, "id", 1L);

        when(userRepository.findByLoginIdWithRelations("u1")).thenReturn(Optional.of(user));
        when(teamRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateMyTeam("u1", RoleLevel.TEAM_MEMBER, 999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("team not found");
    }

    @Test
    void updateMyTeamRejectsRoleElevationWithoutHrAuthority() {
        Branch branch = new Branch("Euljiro", 37.5, 127.0);
        Team root = new Team("Engineering", null, branch);
        Team team = new Team("Platform", root, branch);
        User user = new User("u1", passwordEncoder.encode("password123!"), "u1@test.com", "U1", RoleLevel.TEAM_MEMBER, team, null);
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(team, "id", 10L);

        when(userRepository.findByLoginIdWithRelations("u1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateMyTeam("u1", RoleLevel.MANAGER, 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("cannot elevate own role level without HR authority");
    }

    @Test
    void updateMyTeamAllowsRoleElevationWithHrAuthority() {
        Branch branch = new Branch("Euljiro", 37.5, 127.0);
        Team root = new Team("Engineering", null, branch);
        Team team = new Team("Platform", root, branch);
        User user = new User("u1", passwordEncoder.encode("password123!"), "u1@test.com", "U1", RoleLevel.TEAM_MEMBER, team, null);
        user.grantHrAuthority();
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(team, "id", 10L);

        when(userRepository.findByLoginIdWithRelations("u1")).thenReturn(Optional.of(user));
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userService.updateMyTeam("u1", RoleLevel.MANAGER, 10L);

        assertThat(updated.getRoleLevel()).isEqualTo(RoleLevel.MANAGER);
    }
}
