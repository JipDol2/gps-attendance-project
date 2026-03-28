package com.attendance.attendance.infrastructure;

import com.attendance.attendance.domain.WorkSession;
import com.attendance.attendance.domain.WorkSessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WorkSessionRepository extends JpaRepository<WorkSession, Long> {
    Optional<WorkSession> findFirstByUserIdAndStatusOrderByCheckInAtDesc(Long userId, WorkSessionStatus status);

    List<WorkSession> findByUserIdOrderByCheckInAtDesc(Long userId);

    List<WorkSession> findByUserIdAndCheckInAtBetweenOrderByCheckInAtDesc(Long userId, LocalDateTime from, LocalDateTime to);

    @EntityGraph(attributePaths = {"user"})
    Page<WorkSession> findByUserIdOrderByCheckInAtDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<WorkSession> findByUserIdAndCheckInAtBetweenOrderByCheckInAtDesc(
            Long userId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<WorkSession> findByUserIdInOrderByCheckInAtDesc(List<Long> userIds, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<WorkSession> findByUserIdInAndCheckInAtBetweenOrderByCheckInAtDesc(
            List<Long> userIds, LocalDateTime from, LocalDateTime to, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    List<WorkSession> findByUserIdInAndCheckInAtBetweenOrderByCheckInAtDesc(
            List<Long> userIds, LocalDateTime from, LocalDateTime to);
}
