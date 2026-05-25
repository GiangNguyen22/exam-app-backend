package com.android.app.exam_app_backend.security;

import com.android.app.exam_app_backend.entity.AuditLog;
import com.android.app.exam_app_backend.entity.User;
import com.android.app.exam_app_backend.entity.enums.AuditResult;
import com.android.app.exam_app_backend.repository.AuditLogRepository;
import com.android.app.exam_app_backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("permissionEvaluator")
public class PermissionEvaluatorService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public PermissionEvaluatorService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    public boolean hasPermission(Authentication authentication, String permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean allowed = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(permission));

        if (!allowed) {
            writeDenyLog(authentication.getName(), permission, "Missing required permission");
        }

        return allowed;
    }

    private void writeDenyLog(String username, String permission, String reason) {
        AuditLog auditLog = new AuditLog();
        User user = userRepository.findByUsername(username).orElse(null);
        auditLog.setUser(user);
        auditLog.setAction(permission);
        auditLog.setResourceType("permission");
        auditLog.setResult(AuditResult.deny);
        auditLog.setReason(reason);
        auditLogRepository.save(auditLog);
    }
}
