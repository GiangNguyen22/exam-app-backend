package com.android.app.exam_app_backend.payload;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class ExamCreateRequest {

    @NotBlank
    private String title;

    @Min(1)
    private Integer durationMinutes;

    @NotNull
    private BigDecimal scorePerQuestion;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean shuffleQuestions = true;
    private Boolean shuffleAnswers = true;
    private List<Long> groupIds;
}
