package com.android.app.exam_app_backend.common.constants;

/**
 * Common constants used across the application.
 * Place frequently used strings, messages, and configuration defaults here
 * to prevent hardcoded magic strings.
 */
public final class AppConstants {

    private AppConstants() {
        // Prevent instantiation
    }

    // ==========================================
    // Profile & Environment Constants
    // ==========================================
    public static final String PROFILE_DEV = "dev";
    public static final String PROFILE_PROD = "prod";
    public static final String PROFILE_PRODUCTION = "production";

    // ==========================================
    // Audit Resource Types
    // ==========================================
    public static final String RESOURCE_EXAM = "exam";
    public static final String RESOURCE_EXAM_VERSION = "exam_version";
    public static final String RESOURCE_EXAM_QUESTION = "exam_question";
    public static final String RESOURCE_QUESTION = "question";
    public static final String RESOURCE_USER = "user";
    public static final String RESOURCE_PERMISSION = "permission";
    public static final String RESOURCE_AUTH = "auth";

    // ==========================================
    // Success Messages
    // ==========================================
    public static final String MSG_EXAM_CREATED = "Exam created";
    public static final String MSG_EXAM_UPDATED = "Exam updated";
    public static final String MSG_EXAM_DELETED = "Exam deleted";
    public static final String MSG_EXAMS_RETRIEVED = "Exams retrieved";
    public static final String MSG_EXAM_RETRIEVED = "Exam retrieved";
    public static final String MSG_EXAM_QUESTIONS_RETRIEVED = "Exam questions retrieved";
    public static final String MSG_EXAM_QUESTION_CREATED = "Exam question created";
    public static final String MSG_EXAM_QUESTION_UPDATED = "Exam question updated";
    public static final String MSG_EXAM_SUBMITTED = "Exam submitted";
    public static final String MSG_EXAM_SUBMITTED_FOR_REVIEW = "Exam submitted for review";
    public static final String MSG_EXAM_CHANGES_REQUESTED = "Exam changes requested";
    public static final String MSG_EXAM_VERSIONS_RETRIEVED = "Exam versions retrieved";
    public static final String MSG_EXAM_APPROVED = "Exam approved";
    public static final String MSG_EXAM_REJECTED = "Exam rejected";
    public static final String MSG_EXAM_REVIEW_STARTED = "Exam review started";
    public static final String MSG_EXAM_INTEGRITY_VERIFIED = "Exam integrity verified";
    public static final String MSG_AUDIT_LOGS_RETRIEVED = "Audit logs retrieved";
    public static final String MSG_LOGIN_SUCCESS = "Login successfully";

    // ==========================================
    // Error & Validation Messages
    // ==========================================
    public static final String ERR_UNAUTHORIZED = "Authentication required";
    public static final String ERR_FORBIDDEN_OWNERSHIP = "Bạn không có quyền thao tác trên đề thi này";
    public static final String ERR_MAKER_CHECKER_VIOLATION = "Người tạo đề không được quyền phê duyệt đề thi của chính mình.";
    public static final String ERR_INTEGRITY_COMPROMISED = "Cảnh báo bảo mật: Dữ liệu đề thi đã bị thay đổi trái phép (vi phạm toàn vẹn HMAC-SHA256).";
    public static final String ERR_EXAM_NOT_FOUND = "Exam not found";
    public static final String ERR_USER_NOT_FOUND = "User not found";
}
