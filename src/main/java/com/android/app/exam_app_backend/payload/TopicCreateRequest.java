package com.android.app.exam_app_backend.payload;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TopicCreateRequest {
    private String name;
    private String description;
}
