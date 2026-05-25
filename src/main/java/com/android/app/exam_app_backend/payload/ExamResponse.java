package com.android.app.exam_app_backend.payload;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExamResponse {
    private Long id;
    private String code;
    private String title;
}
