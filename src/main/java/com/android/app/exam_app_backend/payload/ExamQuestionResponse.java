package com.android.app.exam_app_backend.payload;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class ExamQuestionResponse {
    private Long examQuestionId;
    private Long questionId;
    private Integer orderIndex;
    private BigDecimal score;
    private Long subjectId;
    private Long topicId;
    private String content;
    private String imageUrl;
    private String type;
    private String difficulty;
    private List<AnswerOptionResponse> answers;

    @Getter
    @Builder
    public static class AnswerOptionResponse {
        private Long id;
        private String content;
        private Boolean correct;
        private String explanation;
    }
}
