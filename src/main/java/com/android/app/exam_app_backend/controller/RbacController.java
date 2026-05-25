package com.android.app.exam_app_backend.controller;

import com.android.app.exam_app_backend.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rbac")
public class RbacController {

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
}
