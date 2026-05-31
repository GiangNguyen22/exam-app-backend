package com.android.app.exam_app_backend.payload;

import com.android.app.exam_app_backend.entity.enums.Difficulty;
import com.android.app.exam_app_backend.entity.enums.QuestionType;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ExamQuestionCreateRequest {

    @NotNull
    private Long subjectId;

    private Long topicId;

    @NotBlank
    private String content;

    @NotNull
    private QuestionType type;

    @NotNull
    private Difficulty difficulty;

    private Integer orderIndex;

    private BigDecimal score;

    @Valid
    @NotEmpty
    private List<AnswerRequest> answers;

    @Getter
    @Setter
    public static class AnswerRequest {
        @NotBlank
        private String content;

        private Boolean correct = false;

        private String explanation;
    }
}
