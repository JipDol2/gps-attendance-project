package com.attendance.permission.application;

import com.attendance.mobile.presentation.dto.PermissionMemberResponse;
import com.attendance.mobile.presentation.dto.PermissionResponse;
import com.attendance.mobile.presentation.dto.PermissionUpsertRequest;
import com.attendance.permission.domain.Permission;
import com.attendance.permission.domain.UserPermission;
import com.attendance.permission.infrastructure.PermissionRepository;
import com.attendance.permission.infrastructure.UserPermissionRepository;
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
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissions() {
        return permissionRepository.findAll().stream()
                .map(permission -> new PermissionResponse(
                        permission.getId(),
                        permission.getName(),
                        permission.getDescription(),
                        userPermissionRepository.countByPermissionId(permission.getId()),
                        permission.getCreatedAt() == null ? null : permission.getCreatedAt().toLocalDate().toString()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PermissionMemberResponse> permissionMembers(Long permissionId) {
        ensurePermissionExists(permissionId);
        return userPermissionRepository.findByPermissionId(permissionId).stream()
                .map(mapping -> new PermissionMemberResponse(
                        mapping.getUser().getId(),
                        mapping.getUser().getName(),
                        mapping.getUser().getTeam() == null ? null : mapping.getUser().getTeam().getName()
                ))
                .toList();
    }

    public PermissionResponse createPermission(String actorLoginId, PermissionUpsertRequest request) {
        requireHrActor(actorLoginId);
        if (permissionRepository.existsByName(request.name())) {
            throw new BusinessException("permission name already exists");
        }
        Permission saved = permissionRepository.save(new Permission(request.name(), request.description()));
        return new PermissionResponse(saved.getId(), saved.getName(), saved.getDescription(), 0,
                saved.getCreatedAt() == null ? null : saved.getCreatedAt().toLocalDate().toString());
    }

    public PermissionResponse updatePermission(String actorLoginId, Long permissionId, PermissionUpsertRequest request) {
        requireHrActor(actorLoginId);
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new BusinessException("permission not found"));
        if (permissionRepository.existsByNameAndIdNot(request.name(), permissionId)) {
            throw new BusinessException("permission name already exists");
        }
        permission.update(request.name(), request.description());
        Permission saved = permissionRepository.save(permission);
        return new PermissionResponse(
                saved.getId(),
                saved.getName(),
                saved.getDescription(),
                userPermissionRepository.countByPermissionId(saved.getId()),
                saved.getCreatedAt() == null ? null : saved.getCreatedAt().toLocalDate().toString()
        );
    }

    public void deletePermission(String actorLoginId, Long permissionId) {
        requireHrActor(actorLoginId);
        ensurePermissionExists(permissionId);
        userPermissionRepository.deleteByPermissionId(permissionId);
        permissionRepository.deleteById(permissionId);
    }

    public void assignMember(String actorLoginId, Long permissionId, Long userId) {
        requireHrActor(actorLoginId);
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new BusinessException("permission not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("user not found"));
        if (userPermissionRepository.existsByUserIdAndPermissionId(userId, permissionId)) {
            return;
        }
        userPermissionRepository.save(new UserPermission(user, permission));
    }

    public void unassignMember(String actorLoginId, Long permissionId, Long userId) {
        requireHrActor(actorLoginId);
        ensurePermissionExists(permissionId);
        userPermissionRepository.deleteByUserIdAndPermissionId(userId, permissionId);
    }

    private void ensurePermissionExists(Long permissionId) {
        if (!permissionRepository.existsById(permissionId)) {
            throw new BusinessException("permission not found");
        }
    }

    private void requireHrActor(String actorLoginId) {
        User actor = userRepository.findByLoginId(actorLoginId)
                .orElseThrow(() -> new BusinessException("user not found"));
        if (!actor.isHrAuthority()) {
            throw new BusinessException("permission denied");
        }
    }
}
