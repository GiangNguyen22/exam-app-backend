package com.android.app.exam_app_backend.payload;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProctoringEventResponse {
    private Long id;
    private Long examId;
    private Long studentId;
    private String studentName;
    private String username;
    private String eventType;
    private String details;
    private LocalDateTime createdAt;
}
