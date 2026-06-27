package com.android.app.exam_app_backend.payload;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileUploadResponse {
    private String fileName;
    private String url;
}
