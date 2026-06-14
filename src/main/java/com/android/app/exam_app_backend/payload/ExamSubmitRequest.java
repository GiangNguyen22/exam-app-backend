package com.android.app.exam_app_backend.payload;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ExamSubmitRequest {
    private String note;
    private List<StudentAnswerRequest> answers;

    @Getter
    @Setter
    public static class StudentAnswerRequest {
        private Long questionId;
        private List<Long> selectedAnswerIds;
        private String fillContent;
    }
}
