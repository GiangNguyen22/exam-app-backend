package com.android.app.exam_app_backend.controller;

import com.android.app.exam_app_backend.common.dto.ApiResponse;
import com.android.app.exam_app_backend.payload.AuditLogResponse;
import com.android.app.exam_app_backend.security.PermissionConstants;
import com.android.app.exam_app_backend.service.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, '" + PermissionConstants.AUDIT_VIEW + "')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAuditLogs() {
        return ResponseEntity.ok(ApiResponse.<List<AuditLogResponse>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Audit logs retrieved")
                .data(auditLogService.getAuditLogs())
                .build());
    }
}
