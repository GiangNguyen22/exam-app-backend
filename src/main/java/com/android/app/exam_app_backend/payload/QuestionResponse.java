package com.android.app.exam_app_backend.payload;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class QuestionResponse {
    private Long id;
    private Long subjectId;
    private String subjectName;
    private Long topicId;
    private String topicName;
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
