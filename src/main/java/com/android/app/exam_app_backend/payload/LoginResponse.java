package com.android.app.exam_app_backend.payload;

import lombok.Getter;

import java.util.List;

@Getter
public class LoginResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private List<String> roles;

    public LoginResponse(String accessToken, List<String> roles) {
        this.accessToken = accessToken;
        this.roles = roles;
    }
}
