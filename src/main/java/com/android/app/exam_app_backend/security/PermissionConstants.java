package com.android.app.exam_app_backend.security;

public final class PermissionConstants {

    private PermissionConstants() {
    }

    public static final String QUESTION_CREATE = "question:create";
    public static final String QUESTION_VIEW = "question:view";
    public static final String QUESTION_UPDATE = "question:update";
    public static final String QUESTION_DELETE = "question:delete";
    public static final String QUESTION_IMPORT = "question:import";
    public static final String USER_VIEW = "user:view";
    public static final String USER_CREATE = "user:create";
    public static final String USER_UPDATE = "user:update";
    public static final String USER_LOCK = "user:lock";
    public static final String AUDIT_VIEW = "audit:view";
    public static final String EXAM_CREATE = "exam:create";
    public static final String EXAM_DELETE = "exam:delete";
    public static final String EXAM_GENERATE = "exam:generate";
    public static final String EXAM_SUBMIT = "exam:submit";
    public static final String EXAM_VIEW_RESULTS = "exam:viewResults";
    public static final String EXAM_VIEW_OWN_RESULTS = "exam:viewOwnResults";
}
