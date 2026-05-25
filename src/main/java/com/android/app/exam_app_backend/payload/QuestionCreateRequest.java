package com.android.app.exam_app_backend.payload;

import com.android.app.exam_app_backend.entity.enums.Difficulty;
import com.android.app.exam_app_backend.entity.enums.QuestionType;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class QuestionCreateRequest {

    @NotNull
    private Long subjectId;

    private Long topicId;

    @NotBlank
    private String content;

    @NotNull
    private QuestionType type;

    @NotNull
    private Difficulty difficulty;
}
