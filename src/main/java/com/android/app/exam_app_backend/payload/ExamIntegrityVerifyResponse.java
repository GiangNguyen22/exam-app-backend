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
public class ExamIntegrityVerifyResponse {
    private Long examId;
    private Long examVersionId;
    private Integer versionNo;
    private String status;
    private String integrityTag;
    private boolean valid;
    private LocalDateTime verifiedAt;
}
