package com.attendance.permission.infrastructure;

import com.attendance.permission.domain.UserPermission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {
    @EntityGraph(attributePaths = {"user", "user.team", "permission"})
    List<UserPermission> findByPermissionId(Long permissionId);

    int countByPermissionId(Long permissionId);

    boolean existsByUserIdAndPermissionId(Long userId, Long permissionId);

    void deleteByUserIdAndPermissionId(Long userId, Long permissionId);

    void deleteByPermissionId(Long permissionId);
}
