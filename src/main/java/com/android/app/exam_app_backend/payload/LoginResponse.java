package com.android.app.exam_app_backend.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private List<String> roles;

    public LoginResponse(String accessToken, List<String> roles) {
        this.accessToken = accessToken;
        this.roles = roles;
        this.tokenType = "Bearer";
    }

    public LoginResponse(String accessToken, String refreshToken, List<String> roles) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.roles = roles;
        this.tokenType = "Bearer";
    }
}
