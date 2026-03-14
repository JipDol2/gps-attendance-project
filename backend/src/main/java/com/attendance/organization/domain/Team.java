package com.attendance.organization.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "teams",
        uniqueConstraints = @UniqueConstraint(columnNames = {"parent_team_id", "name"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_team_id")
    private Team parentTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    public Team(String name, Team parentTeam) {
        this(name, parentTeam, null);
    }

    public Team(String name, Team parentTeam, Branch branch) {
        this.name = name;
        this.parentTeam = parentTeam;
        this.branch = branch;
    }

    public void changeName(String name) {
        this.name = name;
    }

    public void changeParentTeam(Team parentTeam) {
        this.parentTeam = parentTeam;
    }

    public void changeBranch(Branch branch) {
        this.branch = branch;
    }

    private static final int MAX_HIERARCHY_DEPTH = 50;

    public Long rootTeamId() {
        Team current = this;
        int depth = 0;
        while (current.parentTeam != null) {
            if (++depth > MAX_HIERARCHY_DEPTH) {
                throw new IllegalStateException("team hierarchy depth exceeds maximum (" + MAX_HIERARCHY_DEPTH + "), possible cycle detected");
            }
            current = current.parentTeam;
        }
        return current.id;
    }
}
