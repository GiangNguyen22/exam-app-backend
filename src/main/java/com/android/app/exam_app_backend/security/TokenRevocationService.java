package com.android.app.exam_app_backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenRevocationService {

    private static final Logger log = LoggerFactory.getLogger(TokenRevocationService.class);

    private final Map<String, Long> revokedTokens = new ConcurrentHashMap<>();

    public void revokeToken(String token, Date expirationDate) {
        if (token == null || token.trim().isEmpty()) {
            return;
        }
        long expiryTime = expirationDate != null ? expirationDate.getTime() : System.currentTimeMillis() + 900000L;
        revokedTokens.put(token, expiryTime);
        log.info("Access token revoked until timestamp={}", expiryTime);
    }

    public boolean isTokenRevoked(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        Long expiry = revokedTokens.get(token);
        if (expiry == null) {
            return false;
        }
        if (System.currentTimeMillis() > expiry) {
            revokedTokens.remove(token);
            return false;
        }
        return true;
    }

    public void cleanupExpiredTokens() {
        long now = System.currentTimeMillis();
        revokedTokens.entrySet().removeIf(entry -> entry.getValue() <= now);
    }
}
