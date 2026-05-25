package com.android.app.exam_app_backend.payload;

import com.android.app.exam_app_backend.entity.enums.ExamResultStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class ExamResultResponse {
    private Long resultId;
    private Long examId;
    private BigDecimal score;
    private ExamResultStatus status;
    private LocalDateTime submittedAt;
}
