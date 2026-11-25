package com.android.app.exam_app_backend;

import com.android.app.exam_app_backend.entity.User;
import com.android.app.exam_app_backend.repository.RefreshTokenRepository;
import com.android.app.exam_app_backend.repository.UserRepository;
import com.android.app.exam_app_backend.security.RateLimiterFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.TestPropertySource(properties = "app.security.rate-limit.login-per-minute=20")
class AuthRateLimitSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RateLimiterFilter rateLimiterFilter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void resetRateLimiterAndUser() {
        rateLimiterFilter.clearCounts();
        userRepository.findByUsername("student1").ifPresent(user -> {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        });
    }

    @org.junit.jupiter.api.AfterEach
    void cleanUp() {
        rateLimiterFilter.clearCounts();
    }

    @Test
    @DisplayName("C1-A: Đăng nhập thành công trả về cả Access Token và Refresh Token")
    void testSuccessfulLoginReturnsTokens() throws Exception {
        String loginPayload = "{\"username\":\"student1\",\"password\":\"student123\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString());
    }

    @Test
    @DisplayName("C1-B & C1-C: Refresh Token Rotation và Automatic Reuse Detection")
    void testRefreshTokenRotationAndReuseDetection() throws Exception {
        String loginPayload = "{\"username\":\"student1\",\"password\":\"student123\"}";

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String initialRefreshToken = loginJson.path("data").path("refreshToken").asText();
        assertThat(initialRefreshToken).isNotBlank();

        // 1. Rotate with valid refresh token -> returns new token pair
        String refreshPayload = "{\"refreshToken\":\"" + initialRefreshToken + "\"}";
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andReturn();

        JsonNode refreshJson = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String newRefreshToken = refreshJson.path("data").path("refreshToken").asText();
        assertThat(newRefreshToken).isNotEqualTo(initialRefreshToken);

        // 2. Reuse Detection: Re-sending initialRefreshToken triggers revocation of all tokens and 401
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Refresh token đã bị sử dụng lại")));

        // 3. New token is also now revoked because of reuse detection
        String newRefreshPayload = "{\"refreshToken\":\"" + newRefreshToken + "\"}";
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newRefreshPayload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("C1-D: Gọi /api/auth/logout thu hồi Access Token và Refresh Token ngay lập tức")
    void testLogoutRevokesTokens() throws Exception {
        String loginPayload = "{\"username\":\"teacher1\",\"password\":\"teacher123\"}";

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginJson.path("data").path("accessToken").asText();
        String refreshToken = loginJson.path("data").path("refreshToken").asText();

        // Verify access token works initially
        mockMvc.perform(get("/api/questions")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Call logout
        String logoutPayload = "{\"refreshToken\":\"" + refreshToken + "\"}";
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutPayload))
                .andExpect(status().isOk());

        // Subsequent access with revoked access token must fail with 401
        mockMvc.perform(get("/api/questions")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());

        // Refresh token is also revoked
        String refreshPayload = "{\"refreshToken\":\"" + refreshToken + "\"}";
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshPayload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Brute-Force Lockout: Nhập sai mật khẩu 5 lần thì tài khoản bị khóa")
    void testBruteForceAccountLockout() throws Exception {
        String wrongLoginPayload = "{\"username\":\"student1\",\"password\":\"wrongpassword\"}";

        for (int i = 1; i <= 4; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(wrongLoginPayload))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("còn " + (5 - i) + " lần thử")));
        }

        // 5th attempt locks the account
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wrongLoginPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("bị khóa 15 phút")));

        // 6th attempt (even with correct password) is rejected while locked
        String correctLoginPayload = "{\"username\":\"student1\",\"password\":\"student123\"}";
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(correctLoginPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("đang bị khóa")));
    }

    @Test
    @DisplayName("Rate Limiting: Gửi quá 20 request/phút từ 1 IP bị trả về 429 Too Many Requests")
    void testRateLimitingExceeded() throws Exception {
        String payload = "{\"username\":\"student1\",\"password\":\"wrongpassword\"}";
        String testIp = "192.168.1.99";

        // Send 20 requests
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .header("X-Forwarded-For", testIp)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload));
        }

        // The 21st request must receive 429
        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", testIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(429));
    }
}
