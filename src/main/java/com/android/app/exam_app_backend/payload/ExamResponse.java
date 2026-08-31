package com.android.app.exam_app_backend.payload;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ExamResponse {
    private Long id;
    private String code;
    private String title;
    private Integer durationMinutes;
    private BigDecimal scorePerQuestion;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean shuffleQuestions;
    private Boolean shuffleAnswers;
    private Boolean showAnswersAfterSubmit;
    private List<Long> groupIds;
    private Integer totalQuestions;
}
