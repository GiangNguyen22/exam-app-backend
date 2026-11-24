package com.android.app.exam_app_backend.controller;

import com.android.app.exam_app_backend.common.dto.ApiResponse;
import com.android.app.exam_app_backend.payload.LoginRequest;
import com.android.app.exam_app_backend.payload.LoginResponse;
import com.android.app.exam_app_backend.payload.LogoutRequest;
import com.android.app.exam_app_backend.payload.RefreshTokenRequest;
import com.android.app.exam_app_backend.payload.TokenRefreshResponse;
import com.android.app.exam_app_backend.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login attempt username={}", loginRequest.getUsername());
        LoginResponse response = authService.login(loginRequest);

        return ResponseEntity.ok(
                ApiResponse.<LoginResponse>builder()
                        .success(true)
                        .code(HttpStatus.OK.value())
                        .message("Login successful")
                        .data(response)
                        .build()
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request received");
        TokenRefreshResponse response = authService.refresh(request);

        return ResponseEntity.ok(
                ApiResponse.<TokenRefreshResponse>builder()
                        .success(true)
                        .code(HttpStatus.OK.value())
                        .message("Token refreshed successfully")
                        .data(response)
                        .build()
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) LogoutRequest request) {
        String refreshToken = request != null ? request.getRefreshToken() : null;
        authService.logout(authorization, refreshToken);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .code(HttpStatus.OK.value())
                        .message("Logged out successfully")
                        .data(null)
                        .build()
        );
    }
}
