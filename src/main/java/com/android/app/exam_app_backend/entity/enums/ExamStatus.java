package com.android.app.exam_app_backend.entity.enums;

public enum ExamStatus {
    DRAFT,
    SUBMITTED,
    IN_REVIEW,
    APPROVED,
    REJECTED,
    CHANGES_REQUESTED,
    SCHEDULED,
    RELEASED,
    CLOSED,
    REVOKED;

    public boolean isModifiable() {
        return this == DRAFT;
    }
}
