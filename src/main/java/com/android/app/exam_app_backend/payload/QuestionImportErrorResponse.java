package com.android.app.exam_app_backend.payload;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuestionImportErrorResponse {
    private int rowNumber;
    private String questionKey;
    private String field;
    private String message;
}
