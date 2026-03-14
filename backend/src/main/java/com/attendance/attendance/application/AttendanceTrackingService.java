package com.attendance.attendance.application;

import com.attendance.attendance.domain.WorkPolicy;
import com.attendance.attendance.domain.WorkSession;
import com.attendance.attendance.domain.WorkSessionStatus;
import com.attendance.attendance.infrastructure.WorkPolicyRepository;
import com.attendance.attendance.infrastructure.WorkSessionRepository;
import com.attendance.organization.domain.Branch;
import com.attendance.shared.exception.BusinessException;
import com.attendance.shared.geo.GeoDistanceCalculator;
import com.attendance.user.domain.User;
import com.attendance.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceTrackingService {

    private final UserRepository userRepository;
    private final WorkSessionRepository workSessionRepository;
    private final WorkPolicyRepository workPolicyRepository;
    private final GeoDistanceCalculator geoDistanceCalculator;

    public TrackingResult processMyLocation(String loginId, double latitude, double longitude, LocalDateTime observedAt) {
        User user = userRepository.findByLoginIdWithRelations(loginId)
                .orElseThrow(() -> new BusinessException("user not found"));
        return processLocationForUser(user, latitude, longitude, observedAt);
    }

    public TrackingResult processLocation(Long userId, double latitude, double longitude, LocalDateTime observedAt) {
        User user = userRepository.findByIdWithRelations(userId)
                .orElseThrow(() -> new BusinessException("user not found"));
        return processLocationForUser(user, latitude, longitude, observedAt);
    }

    private TrackingResult processLocationForUser(User user, double latitude, double longitude, LocalDateTime observedAt) {
        WorkPolicy policy = resolveUserPolicy(user);
        Branch branch = resolveBranch(user);

        double distance = geoDistanceCalculator.meters(
                branch.getLatitude(),
                branch.getLongitude(),
                latitude,
                longitude
        );

        Optional<WorkSession> activeSessionOpt = workSessionRepository
                .findFirstByUserIdAndStatusOrderByCheckInAtDesc(user.getId(), WorkSessionStatus.CHECKED_IN);

        if (activeSessionOpt.isEmpty()) {
            if (distance <= policy.getCheckinRadiusM()) {
                WorkSession session = new WorkSession(user, policy, observedAt, latitude, longitude);
                workSessionRepository.save(session);
                return TrackingResult.of(TrackingState.CHECKED_IN_STARTED, "checked in", session, distance);
            }
            return TrackingResult.of(TrackingState.OUTSIDE_CHECKIN_RADIUS, "outside checkin radius", null, distance);
        }

        WorkSession activeSession = activeSessionOpt.get();
        activeSession.updateLastLocation(latitude, longitude);

        if (distance > policy.getCheckoutRadiusM()) {
            if (activeSession.getOutsideSince() == null) {
                activeSession.markOutside(observedAt);
                workSessionRepository.save(activeSession);
                return TrackingResult.of(
                        TrackingState.OUTSIDE_CHECKOUT_RADIUS_TIMER_STARTED,
                        "outside checkout radius, timer started",
                        activeSession,
                        distance
                );
            }

            LocalDateTime threshold = activeSession.getOutsideSince().plusMinutes(policy.getCheckoutGraceMinutes());
            if (!observedAt.isBefore(threshold)) {
                activeSession.checkout(observedAt, latitude, longitude);
                workSessionRepository.save(activeSession);
                return TrackingResult.of(TrackingState.CHECKED_OUT_AUTOMATIC, "checked out automatically", activeSession, distance);
            }

            workSessionRepository.save(activeSession);
            return TrackingResult.of(TrackingState.OUTSIDE_CHECKOUT_RADIUS, "outside checkout radius", activeSession, distance);
        }

        if (activeSession.getOutsideSince() != null) {
            activeSession.clearOutsideState();
            workSessionRepository.save(activeSession);
            return TrackingResult.of(TrackingState.BACK_INSIDE_CHECKOUT_RADIUS, "back inside checkout radius", activeSession, distance);
        }

        workSessionRepository.save(activeSession);
        return TrackingResult.of(TrackingState.STILL_CHECKED_IN, "still checked in", activeSession, distance);
    }

    private WorkPolicy resolveUserPolicy(User user) {
        if (user.getWorkPolicy() != null) {
            return user.getWorkPolicy();
        }
        if (user.getTeam() == null) {
            throw new BusinessException("team is not assigned");
        }
        WorkPolicy policy = workPolicyRepository.findFirstByTeamIdOrderByIdDesc(user.getTeam().getId())
                .orElseThrow(() -> new BusinessException("work policy is not assigned for your team"));
        user.assignWorkPolicy(policy);
        userRepository.save(user);
        return policy;
    }

    private Branch resolveBranch(User user) {
        if (user.getTeam() == null) {
            throw new BusinessException("team is not assigned");
        }
        if (user.getTeam().getBranch() == null) {
            throw new BusinessException("branch is not assigned for your team");
        }
        return user.getTeam().getBranch();
    }

    public record TrackingResult(
            TrackingState state,
            String message,
            Long sessionId,
            Double distanceM
    ) {
        static TrackingResult of(TrackingState state, String message, WorkSession session, double distanceM) {
            return new TrackingResult(state, message, session == null ? null : session.getId(), distanceM);
        }
    }
}
