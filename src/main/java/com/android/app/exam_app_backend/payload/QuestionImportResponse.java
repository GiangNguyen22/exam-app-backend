package com.android.app.exam_app_backend.payload;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class QuestionImportResponse {
    private boolean success;
    private int totalGroups;
    private int importedQuestions;
    private int importedAnswers;
    private List<QuestionImportErrorResponse> errors;
}
