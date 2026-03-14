package com.attendance.attendance.domain;

import com.attendance.organization.domain.Team;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "work_policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "checkin_radius_m", nullable = false)
    private int checkinRadiusM;

    @Column(name = "checkout_radius_m", nullable = false)
    private int checkoutRadiusM;

    @Column(name = "checkout_grace_minutes", nullable = false)
    private int checkoutGraceMinutes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id")
    private Team team;

    public WorkPolicy(
            String name,
            int checkinRadiusM,
            int checkoutRadiusM,
            int checkoutGraceMinutes,
            Team team
    ) {
        this.name = name;
        this.checkinRadiusM = checkinRadiusM;
        this.checkoutRadiusM = checkoutRadiusM;
        this.checkoutGraceMinutes = checkoutGraceMinutes;
        this.team = team;
    }

    public void update(
            Team team,
            String name,
            int checkinRadiusM,
            int checkoutRadiusM,
            int checkoutGraceMinutes
    ) {
        this.team = team;
        this.name = name;
        this.checkinRadiusM = checkinRadiusM;
        this.checkoutRadiusM = checkoutRadiusM;
        this.checkoutGraceMinutes = checkoutGraceMinutes;
    }
}
