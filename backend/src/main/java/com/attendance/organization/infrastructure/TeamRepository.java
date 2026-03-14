package com.attendance.organization.infrastructure;

import com.attendance.organization.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findAllByOrderByNameAsc();

    boolean existsByParentTeamAndName(Team parentTeam, String name);

    boolean existsByParentTeamIsNullAndName(String name);

    boolean existsByParentTeamAndNameAndIdNot(Team parentTeam, String name, Long id);

    boolean existsByParentTeamIsNullAndNameAndIdNot(String name, Long id);

    boolean existsByBranchId(Long branchId);
}
