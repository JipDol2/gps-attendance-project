package com.attendance.permission.infrastructure;

import com.attendance.permission.domain.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
}

