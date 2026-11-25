package com.android.app.exam_app_backend.security.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class CryptoKeyProvider {

    private final String configuredKey;
    private SecretKey examSecretKey;

    public CryptoKeyProvider(@Value("${app.security.encryption.exam-key:}") String configuredKey) {
        this.configuredKey = configuredKey;
    }

    @PostConstruct
    public void init() {
        byte[] keyBytes;
        if (configuredKey == null || configuredKey.trim().isEmpty()) {
            throw new IllegalStateException("EXAM_ENCRYPTION_KEY is not configured.");
        }
        String key = configuredKey.trim();
        if (key.length() == 64 && isHex(key)) {
            keyBytes = hexStringToByteArray(key);
        } else if (key.getBytes(StandardCharsets.UTF_8).length >= 32) {
            byte[] raw = key.getBytes(StandardCharsets.UTF_8);
            keyBytes = new byte[32];
            System.arraycopy(raw, 0, keyBytes, 0, 32);
        } else {
            // SHA-256 derive to ensure 256 bits if user passed shorter string
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                keyBytes = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("Failed to derive encryption key", e);
            }
        }
        this.examSecretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public SecretKey getExamSecretKey() {
        return examSecretKey;
    }

    private static boolean isHex(String s) {
        for (char c : s.toCharArray()) {
            if (Character.digit(c, 16) == -1) {
                return false;
            }
        }
        return true;
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
