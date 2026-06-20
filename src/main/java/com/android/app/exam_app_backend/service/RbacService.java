package com.android.app.exam_app_backend.service;

import com.android.app.exam_app_backend.entity.Permission;
import com.android.app.exam_app_backend.entity.Role;
import com.android.app.exam_app_backend.exception.ResourceNotFoundException;
import com.android.app.exam_app_backend.payload.PermissionResponse;
import com.android.app.exam_app_backend.payload.RoleResponse;
import com.android.app.exam_app_backend.repository.PermissionRepository;
import com.android.app.exam_app_backend.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RbacService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RbacService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getRoles() {
        return roleRepository.findAllWithPermissions().stream()
                .map(this::toRoleResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> getPermissions() {
        return permissionRepository.findAll().stream()
                .sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName()))
                .map(this::toPermissionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RoleResponse updateRolePermissions(Long roleId, List<String> permissionNames) {
        Role role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleId));

        Set<Permission> permissions = permissionNames.stream()
                .map(this::normalizePermissionName)
                .distinct()
                .map(name -> permissionRepository.findByName(name)
                        .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + name)))
                .collect(Collectors.toCollection(HashSet::new));

        role.setPermissions(permissions);
        return toRoleResponse(roleRepository.save(role));
    }

    private RoleResponse toRoleResponse(Role role) {
        List<String> permissions = role.getPermissions().stream()
                .map(Permission::getName)
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());

        return RoleResponse.builder()
                .id(role.getId())
                .name(normalizeRoleName(role.getName()))
                .description(role.getDescription())
                .permissions(permissions)
                .build();
    }

    private PermissionResponse toPermissionResponse(Permission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .build();
    }

    private String normalizeRoleName(String roleName) {
        String normalized = roleName == null ? "" : roleName.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized.substring("ROLE_".length()) : normalized;
    }

    private String normalizePermissionName(String permissionName) {
        return permissionName == null ? "" : permissionName.trim();
    }
}
