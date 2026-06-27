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
public class ExamGenerateRequest {

    @NotBlank
    private String title;

    @Min(1)
    private Integer durationMinutes;

    @NotNull
    private BigDecimal scorePerQuestion;

    private Long subjectId;

    private Long topicId;

    @Min(0)
    private Integer easyCount = 0;

    @Min(0)
    private Integer mediumCount = 0;

    @Min(0)
    private Integer hardCount = 0;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private List<Long> groupIds;
}
