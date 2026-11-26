package com.android.app.exam_app_backend.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamApprovalResponse {
    private Long id;
    private Long examId;
    private Long examVersionId;
    private Integer versionNo;
    private Long approverId;
    private String approverUsername;
    private String approverFullName;
    private Long creatorId;
    private String decision;
    private String comment;
    private LocalDateTime decidedAt;
    private String integrityTag;
    private String examStatus;
}
