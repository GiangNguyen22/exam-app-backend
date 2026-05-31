package com.android.app.exam_app_backend.payload;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubjectResponse {
    private Long id;
    private String name;
    private String description;
}
