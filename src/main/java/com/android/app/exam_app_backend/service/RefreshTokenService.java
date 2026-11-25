package com.android.app.exam_app_backend.service;

import com.android.app.exam_app_backend.entity.RefreshToken;
import com.android.app.exam_app_backend.entity.User;
import com.android.app.exam_app_backend.payload.TokenRefreshResponse;
import com.android.app.exam_app_backend.repository.RefreshTokenRepository;
import com.android.app.exam_app_backend.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider tokenProvider;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtTokenProvider tokenProvider) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenProvider = tokenProvider;
    }

    public String hashToken(String rawToken) {
        if (rawToken == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private String generateSecureToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    @Transactional
    public String createRefreshToken(User user) {
        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);

        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .build();

        refreshTokenRepository.save(entity);
        return rawToken;
    }

    @Transactional(noRollbackFor = {BadCredentialsException.class})
    public TokenRefreshResponse rotateRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.trim().isEmpty()) {
            throw new BadCredentialsException("Refresh token không được để trống");
        }

        String tokenHash = hashToken(rawRefreshToken.trim());
        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadCredentialsException("Refresh token không hợp lệ hoặc không tồn tại"));

        // Automatic Reuse Detection: if token is already revoked or replaced, revoke everything for this user!
        if (token.getRevokedAt() != null || token.getReplacedByTokenHash() != null) {
            log.error("REFRESH_TOKEN_REUSE_DETECTED: Stolen refresh token attempted for userId={}, username={}",
                    token.getUser().getId(), token.getUser().getUsername());
            refreshTokenRepository.revokeAllUserTokens(token.getUser().getId(), LocalDateTime.now());
            throw new BadCredentialsException("Phát hiện Refresh token đã bị sử dụng lại. Toàn bộ phiên đăng nhập đã bị thu hồi vì lý do an toàn.");
        }

        if (token.isExpired()) {
            throw new BadCredentialsException("Refresh token đã hết hạn. Vui lòng đăng nhập lại.");
        }

        // Generate new rotation token pair
        String newRawToken = generateSecureToken();
        String newTokenHash = hashToken(newRawToken);

        token.setRevokedAt(LocalDateTime.now());
        token.setReplacedByTokenHash(newTokenHash);
        refreshTokenRepository.save(token);

        RefreshToken newEntity = RefreshToken.builder()
                .user(token.getUser())
                .tokenHash(newTokenHash)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .build();
        refreshTokenRepository.save(newEntity);

        String newAccessToken = tokenProvider.generateTokenFromUsername(token.getUser().getUsername());
        return new TokenRefreshResponse(newAccessToken, newRawToken);
    }

    @Transactional
    public void revokeToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.trim().isEmpty()) {
            return;
        }
        String tokenHash = hashToken(rawRefreshToken.trim());
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllUserTokens(userId, LocalDateTime.now());
    }
}
