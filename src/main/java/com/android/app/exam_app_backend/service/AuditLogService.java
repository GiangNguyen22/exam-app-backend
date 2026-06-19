package com.android.app.exam_app_backend.service;

import com.android.app.exam_app_backend.entity.AuditLog;
import com.android.app.exam_app_backend.entity.User;
import com.android.app.exam_app_backend.entity.enums.AuditResult;
import com.android.app.exam_app_backend.payload.AuditLogResponse;
import com.android.app.exam_app_backend.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(User user, String action, String resourceType, Long resourceId, AuditResult result, String reason) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUser(user);
        auditLog.setAction(action);
        auditLog.setResourceType(resourceType);
        auditLog.setResourceId(resourceId);
        auditLog.setResult(result);
        auditLog.setReason(reason);
        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogs() {
        return auditLogRepository.findAllWithUserOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        User user = auditLog.getUser();
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .userId(user == null ? null : user.getId())
                .username(user == null ? null : user.getUsername())
                .action(auditLog.getAction())
                .resourceType(auditLog.getResourceType())
                .resourceId(auditLog.getResourceId())
                .result(auditLog.getResult() == null ? null : auditLog.getResult().name())
                .reason(auditLog.getReason())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
