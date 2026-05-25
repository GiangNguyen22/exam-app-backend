package com.android.app.exam_app_backend.service;

import com.android.app.exam_app_backend.entity.AuditLog;
import com.android.app.exam_app_backend.entity.User;
import com.android.app.exam_app_backend.entity.enums.AuditResult;
import com.android.app.exam_app_backend.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

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
}
