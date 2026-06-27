package com.android.app.exam_app_backend.payload;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudentGroupMemberResponse {
    private Long id;
    private Long groupId;
    private Long userId;
    private String username;
    private String fullName;
}
