package com.android.app.exam_app_backend.service;

import com.android.app.exam_app_backend.entity.AuditLog;
import com.android.app.exam_app_backend.entity.User;
import com.android.app.exam_app_backend.entity.enums.AuditResult;
import com.android.app.exam_app_backend.payload.AuditLogResponse;
import com.android.app.exam_app_backend.repository.AuditLogRepository;
import com.android.app.exam_app_backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AuditLogService {

    public static final String ACTION_CREATE_EXAM = "CREATE_EXAM";
    public static final String ACTION_SUBMIT_REVIEW = "SUBMIT_REVIEW";
    public static final String ACTION_START_REVIEW = "START_REVIEW";
    public static final String ACTION_APPROVE_EXAM = "APPROVE_EXAM";
    public static final String ACTION_REJECT_EXAM = "REJECT_EXAM";
    public static final String ACTION_REQUEST_CHANGES_EXAM = "REQUEST_CHANGES_EXAM";
    public static final String ACTION_RELEASE_EXAM = "RELEASE_EXAM";
    public static final String ACTION_REVOKE_EXAM = "REVOKE_EXAM";
    public static final String ACTION_INTEGRITY_MISMATCH = "INTEGRITY_MISMATCH";
    public static final String ACTION_INTEGRITY_VERIFIED = "INTEGRITY_VERIFIED";
    public static final String ACTION_UNAUTHORIZED_ACCESS = "UNAUTHORIZED_ACCESS";
    public static final String ACTION_MAKER_CHECKER_VIOLATION = "MAKER_CHECKER_VIOLATION";

    private static final int MAX_REASON_LENGTH = 1000;
    private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)Bearer\\s+[A-Za-z0-9\\-._~+/]+=*");
    private static final Pattern SECRET_KV_PATTERN = Pattern.compile("(?i)(password|pwd|secret|token|key)\\s*[:=]\\s*[^\\s,;]+");
    private static final Pattern ENC_FIELD_PATTERN = Pattern.compile("ENC:[A-Za-z0-9+/=]+");

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    public void log(User user, String action, String resourceType, Long resourceId, AuditResult result, String reason) {
        saveLog(user, action, resourceType, resourceId, result, reason, null);
    }

    public void log(User user, String action, String resourceType, Long resourceId, AuditResult result, String reason, String meta) {
        saveLog(user, action, resourceType, resourceId, result, reason, meta);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSecurityAlert(Long userId, String action, String resourceType, Long resourceId, AuditResult result, String reason) {
        User actor = userId == null ? null : userRepository.findById(userId).orElse(null);
        saveLog(actor, action, resourceType, resourceId, result, reason, null);
    }

    private void saveLog(User user, String action, String resourceType, Long resourceId, AuditResult result, String reason, String meta) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUser(user);
        auditLog.setAction(action);
        auditLog.setResourceType(resourceType);
        auditLog.setResourceId(resourceId);
        auditLog.setResult(result);
        auditLog.setReason(sanitizeReason(reason));
        auditLog.setMeta(meta);
        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogs() {
        return auditLogRepository.findAllWithUserOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogs(String action, String resourceType, Long resourceId) {
        String normalizedAction = (action == null || action.isBlank()) ? null : action.trim();
        String normalizedResourceType = (resourceType == null || resourceType.isBlank()) ? null : resourceType.trim();
        if (normalizedAction == null && normalizedResourceType == null && resourceId == null) {
            return getAuditLogs();
        }
        return auditLogRepository.searchLogs(normalizedAction, normalizedResourceType, resourceId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private String sanitizeReason(String reason) {
        if (reason == null) {
            return null;
        }
        String sanitized = BEARER_PATTERN.matcher(reason).replaceAll("Bearer [REDACTED]");
        sanitized = SECRET_KV_PATTERN.matcher(sanitized).replaceAll("$1=[REDACTED]");
        sanitized = ENC_FIELD_PATTERN.matcher(sanitized).replaceAll("ENC:[REDACTED]");
        if (sanitized.length() > MAX_REASON_LENGTH) {
            return sanitized.substring(0, MAX_REASON_LENGTH) + "...[TRUNCATED]";
        }
        return sanitized;
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
