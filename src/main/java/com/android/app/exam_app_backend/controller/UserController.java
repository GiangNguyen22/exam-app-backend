package com.android.app.exam_app_backend.controller;

import com.android.app.exam_app_backend.common.dto.ApiResponse;
import com.android.app.exam_app_backend.entity.enums.UserStatus;
import com.android.app.exam_app_backend.payload.UserCreateRequest;
import com.android.app.exam_app_backend.payload.UserProfileResponse;
import com.android.app.exam_app_backend.payload.UserRolesRequest;
import com.android.app.exam_app_backend.payload.UserUpdateRequest;
import com.android.app.exam_app_backend.security.PermissionConstants;
import com.android.app.exam_app_backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, '" + PermissionConstants.USER_VIEW + "')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> getUsers() {
        return ResponseEntity.ok(ApiResponse.<List<UserProfileResponse>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Users retrieved")
                .data(userService.getUsers())
                .build());
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.<UserProfileResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("User profile retrieved")
                .data(userService.getCurrentUserProfile(authentication.getName()))
                .build());
    }

    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, '" + PermissionConstants.USER_CREATE + "')")
    @PostMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> createUser(
            @Valid @RequestBody UserCreateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<UserProfileResponse>builder()
                .success(true)
                .code(HttpStatus.CREATED.value())
                .message("User created")
                .data(userService.createUser(request, authentication.getName()))
                .build());
    }

    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, '" + PermissionConstants.USER_UPDATE + "')")
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.<UserProfileResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("User updated")
                .data(userService.updateUser(userId, request, authentication.getName()))
                .build());
    }

    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, '" + PermissionConstants.USER_LOCK + "')")
    @PutMapping("/{userId}/lock")
    public ResponseEntity<ApiResponse<UserProfileResponse>> lockUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.<UserProfileResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("User locked")
                .data(userService.updateUserStatus(userId, UserStatus.LOCKED))
                .build());
    }

    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, '" + PermissionConstants.USER_LOCK + "')")
    @PutMapping("/{userId}/unlock")
    public ResponseEntity<ApiResponse<UserProfileResponse>> unlockUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.<UserProfileResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("User unlocked")
                .data(userService.updateUserStatus(userId, UserStatus.ACTIVE))
                .build());
    }

    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, '" + PermissionConstants.USER_UPDATE + "')")
    @PutMapping("/{userId}/roles")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUserRoles(
            @PathVariable Long userId,
            @Valid @RequestBody UserRolesRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.<UserProfileResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("User roles updated")
                .data(userService.updateUserRoles(userId, request.getRoles(), authentication.getName()))
                .build());
    }
}
