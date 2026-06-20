package com.android.app.exam_app_backend.controller;

import com.android.app.exam_app_backend.common.dto.ApiResponse;
import com.android.app.exam_app_backend.payload.PermissionResponse;
import com.android.app.exam_app_backend.payload.RolePermissionsUpdateRequest;
import com.android.app.exam_app_backend.payload.RoleResponse;
import com.android.app.exam_app_backend.security.PermissionConstants;
import com.android.app.exam_app_backend.service.RbacService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/rbac")
public class RbacController {

    private final RbacService rbacService;

    public RbacController(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<String>> ping() {
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Authenticated")
                .data("pong")
                .build());
    }

    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'question:create')")
    @GetMapping("/question-create-check")
    public ResponseEntity<ApiResponse<String>> questionCreateCheck() {
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Permission granted")
                .data("question:create")
                .build());
    }

    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, '" + PermissionConstants.RBAC_MANAGE + "')")
    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getRoles() {
        return ResponseEntity.ok(ApiResponse.<List<RoleResponse>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Roles retrieved")
                .data(rbacService.getRoles())
                .build());
    }

    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, '" + PermissionConstants.RBAC_MANAGE + "')")
    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getPermissions() {
        return ResponseEntity.ok(ApiResponse.<List<PermissionResponse>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Permissions retrieved")
                .data(rbacService.getPermissions())
                .build());
    }

    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, '" + PermissionConstants.RBAC_MANAGE + "')")
    @PutMapping("/roles/{roleId}/permissions")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRolePermissions(
            @PathVariable Long roleId,
            @Valid @RequestBody RolePermissionsUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.<RoleResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Role permissions updated")
                .data(rbacService.updateRolePermissions(roleId, request.getPermissions()))
                .build());
    }
}
