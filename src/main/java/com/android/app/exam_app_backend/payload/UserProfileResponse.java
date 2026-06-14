package com.android.app.exam_app_backend.payload;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserProfileResponse {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String studentId;
    private String employeeCode;
    private String status;
    private List<String> roles;
}
