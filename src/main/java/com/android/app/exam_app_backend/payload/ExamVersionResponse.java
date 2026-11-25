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
public class ExamVersionResponse {
    private Long id;
    private Long examId;
    private Integer versionNo;
    private String status;
    private String notes;
    private String integrityTag;
    private LocalDateTime createdAt;
}
