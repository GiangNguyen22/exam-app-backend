package com.android.app.exam_app_backend.payload;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProctoringEventRequest {
    private String eventType;
    private String details;
}
