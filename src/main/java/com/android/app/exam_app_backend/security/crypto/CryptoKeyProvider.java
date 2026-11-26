package com.android.app.exam_app_backend.security.crypto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

@Component
public class CryptoKeyProvider {

    private final String configuredKey;
    private final String configuredIntegrityKey;
    private SecretKey examSecretKey;
    private SecretKey integritySecretKey;

    @Autowired
    public CryptoKeyProvider(
            @Value("${app.security.encryption.exam-key:}") String configuredKey,
            @Value("${app.security.integrity.hmac-key:}") String configuredIntegrityKey) {
        this.configuredKey = configuredKey;
        this.configuredIntegrityKey = configuredIntegrityKey;
    }

    @PostConstruct
    public void init() {
        byte[] examKeyBytes = deriveKeyBytes(configuredKey, "EXAM_ENCRYPTION_KEY");
        byte[] integrityKeyBytes = deriveKeyBytes(configuredIntegrityKey, "INTEGRITY_SECRET_KEY");

        if (Arrays.equals(examKeyBytes, integrityKeyBytes)) {
            throw new IllegalStateException("INTEGRITY_SECRET_KEY must be distinct from EXAM_ENCRYPTION_KEY.");
        }

        this.examSecretKey = new SecretKeySpec(examKeyBytes, "AES");
        this.integritySecretKey = new SecretKeySpec(integrityKeyBytes, "HmacSHA256");
    }

    public SecretKey getExamSecretKey() {
        return examSecretKey;
    }

    public SecretKey getIntegritySecretKey() {
        return integritySecretKey;
    }

    private byte[] deriveKeyBytes(String rawKey, String keyName) {
        if (rawKey == null || rawKey.trim().isEmpty()) {
            throw new IllegalStateException(keyName + " is not configured.");
        }
        String key = rawKey.trim();
        if (key.length() == 64 && isHex(key)) {
            return hexStringToByteArray(key);
        }
        if (key.getBytes(StandardCharsets.UTF_8).length >= 32) {
            byte[] raw = key.getBytes(StandardCharsets.UTF_8);
            byte[] keyBytes = new byte[32];
            System.arraycopy(raw, 0, keyBytes, 0, 32);
            return keyBytes;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(key.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to derive key for " + keyName, e);
        }
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
