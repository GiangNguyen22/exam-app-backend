package com.android.app.exam_app_backend.security.crypto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Converter
@Component
@Slf4j
public class AesGcmAttributeConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12; // 96 bits recommended for GCM
    private static final int GCM_TAG_LENGTH_BITS = 128; // 128 bits auth tag
    public static final String ENCRYPTED_PREFIX = "ENC:";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static CryptoKeyProvider staticKeyProvider;

    private final CryptoKeyProvider keyProvider;

    public AesGcmAttributeConverter() {
        this.keyProvider = staticKeyProvider;
    }

    @Autowired
    public AesGcmAttributeConverter(CryptoKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
        staticKeyProvider = keyProvider;
    }

    private CryptoKeyProvider resolveKeyProvider() {
        return this.keyProvider != null ? this.keyProvider : staticKeyProvider;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            CryptoKeyProvider provider = resolveKeyProvider();
            if (provider == null || provider.getExamSecretKey() == null) {
                log.warn("CryptoKeyProvider not available, storing plaintext as fallback");
                return attribute;
            }

            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, provider.getExamSecretKey(), parameterSpec);

            byte[] plaintextBytes = attribute.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = cipher.doFinal(plaintextBytes);

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);

            return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("Failed to encrypt database column", e);
            throw new RuntimeException("Database field encryption failed", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        // Backward compatibility: If data was stored before encryption or not prefixed with ENC:
        if (!dbData.startsWith(ENCRYPTED_PREFIX)) {
            return dbData;
        }

        try {
            CryptoKeyProvider provider = resolveKeyProvider();
            if (provider == null || provider.getExamSecretKey() == null) {
                log.error("CryptoKeyProvider not available for decryption");
                return dbData;
            }

            String base64Payload = dbData.substring(ENCRYPTED_PREFIX.length());
            byte[] combined = Base64.getDecoder().decode(base64Payload);

            if (combined.length < GCM_IV_LENGTH_BYTES) {
                log.warn("Corrupted encrypted payload, length is less than IV size");
                return dbData;
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(combined);
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            byteBuffer.get(iv);

            byte[] ciphertext = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, provider.getExamSecretKey(), parameterSpec);

            byte[] decryptedBytes = cipher.doFinal(ciphertext);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to decrypt database column, returning fallback", e);
            return dbData;
        }
    }
}
