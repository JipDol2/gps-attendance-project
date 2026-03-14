package com.attendance.organization.application;

import com.attendance.attendance.domain.WorkPolicy;
import com.attendance.attendance.infrastructure.WorkPolicyRepository;
import com.attendance.organization.domain.Branch;
import com.attendance.organization.domain.Team;
import com.attendance.organization.infrastructure.BranchRepository;
import com.attendance.organization.infrastructure.TeamRepository;
import com.attendance.shared.exception.BusinessException;
import com.attendance.user.domain.User;
import com.attendance.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationCommandService {

    private final TeamRepository teamRepository;
    private final BranchRepository branchRepository;
    private final WorkPolicyRepository workPolicyRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Team> listTeams() {
        return teamRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Branch> listBranches() {
        return branchRepository.findAllByOrderByNameAsc();
    }

    public Branch createBranch(String actorLoginId, String name, double latitude, double longitude) {
        requireHrActor(actorLoginId);
        if (branchRepository.existsByName(name)) {
            throw new BusinessException("branch already exists");
        }
        return branchRepository.save(new Branch(name, latitude, longitude));
    }

    public Branch updateBranch(String actorLoginId, Long branchId, String name, double latitude, double longitude) {
        requireHrActor(actorLoginId);
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BusinessException("branch not found"));
        if (branchRepository.existsByNameAndIdNot(name, branchId)) {
            throw new BusinessException("branch already exists");
        }
        branch.update(name, latitude, longitude);
        return branchRepository.save(branch);
    }

    public void deleteBranch(String actorLoginId, Long branchId) {
        requireHrActor(actorLoginId);
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BusinessException("branch not found"));
        if (teamRepository.existsByBranchId(branchId)) {
            throw new BusinessException("cannot delete branch with assigned teams");
        }
        branchRepository.delete(branch);
    }

    public Team createTeam(String actorLoginId, Long parentTeamId, String name, Long branchId) {
        requireHrActor(actorLoginId);
        Team parentTeam = null;
        if (parentTeamId != null) {
            parentTeam = teamRepository.findById(parentTeamId)
                    .orElseThrow(() -> new BusinessException("parent team not found"));
        }
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BusinessException("branch not found"));

        boolean exists = parentTeam == null
                ? teamRepository.existsByParentTeamIsNullAndName(name)
                : teamRepository.existsByParentTeamAndName(parentTeam, name);

        if (exists) {
            throw new BusinessException("team already exists in the same level");
        }

        return teamRepository.save(new Team(name, parentTeam, branch));
    }

    public Team updateTeam(String actorLoginId, Long teamId, String name, Long parentTeamId, Long branchId) {
        requireHrActor(actorLoginId);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException("team not found"));

        Team parentTeam = null;
        if (parentTeamId != null) {
            parentTeam = teamRepository.findById(parentTeamId)
                    .orElseThrow(() -> new BusinessException("parent team not found"));
            if (team.getId().equals(parentTeam.getId())) {
                throw new BusinessException("team cannot be parent of itself");
            }
            if (isDescendantOrSame(parentTeam, team)) {
                throw new BusinessException("cyclic team hierarchy is not allowed");
            }
        }

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BusinessException("branch not found"));

        boolean exists = parentTeam == null
                ? teamRepository.existsByParentTeamIsNullAndNameAndIdNot(name, teamId)
                : teamRepository.existsByParentTeamAndNameAndIdNot(parentTeam, name, teamId);
        if (exists) {
            throw new BusinessException("team already exists in the same level");
        }

        team.changeName(name);
        team.changeParentTeam(parentTeam);
        team.changeBranch(branch);
        return teamRepository.save(team);
    }

    public WorkPolicy createWorkPolicy(
            String actorLoginId,
            Long teamId,
            String name,
            int checkinRadiusM,
            int checkoutRadiusM,
            int checkoutGraceMinutes
    ) {
        requireHrActor(actorLoginId);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException("team not found"));

        validatePolicyValues(checkinRadiusM, checkoutRadiusM, checkoutGraceMinutes);

        WorkPolicy policy = new WorkPolicy(
                name,
                checkinRadiusM,
                checkoutRadiusM,
                checkoutGraceMinutes,
                team
        );
        return workPolicyRepository.save(policy);
    }

    public WorkPolicy updateWorkPolicy(
            String actorLoginId,
            Long policyId,
            Long teamId,
            String name,
            int checkinRadiusM,
            int checkoutRadiusM,
            int checkoutGraceMinutes
    ) {
        requireHrActor(actorLoginId);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException("team not found"));
        WorkPolicy policy = workPolicyRepository.findById(policyId)
                .orElseThrow(() -> new BusinessException("work policy not found"));

        validatePolicyValues(checkinRadiusM, checkoutRadiusM, checkoutGraceMinutes);

        policy.update(team, name, checkinRadiusM, checkoutRadiusM, checkoutGraceMinutes);
        return workPolicyRepository.save(policy);
    }

    private boolean isDescendantOrSame(Team candidate, Team baseTeam) {
        Team current = candidate;
        while (current != null) {
            if (current.getId().equals(baseTeam.getId())) {
                return true;
            }
            current = current.getParentTeam();
        }
        return false;
    }

    private void requireHrActor(String actorLoginId) {
        User actor = userRepository.findByLoginId(actorLoginId)
                .orElseThrow(() -> new BusinessException("user not found"));
        if (!actor.isHrAuthority()) {
            throw new BusinessException("permission denied");
        }
    }

    private void validatePolicyValues(int checkinRadiusM, int checkoutRadiusM, int checkoutGraceMinutes) {
        if (checkinRadiusM <= 0 || checkoutRadiusM <= 0 || checkoutGraceMinutes <= 0) {
            throw new BusinessException("radius and grace minute must be positive");
        }
    }
}
