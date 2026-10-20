package com.android.app.exam_app_backend.config;

import com.google.genai.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình Google Gen AI Java SDK Client (Gemini).
 * API key đọc từ biến môi trường GEMINI_API_KEY (hoặc app.ai.gemini.api-key).
 */
@Configuration
public class GeminiClientConfig {

    private static final Logger log = LoggerFactory.getLogger(GeminiClientConfig.class);

    @Bean
    public Client geminiClient(@Value("${app.ai.gemini.api-key:}") String apiKey) {
        String normalizedKey = apiKey == null ? "" : apiKey.trim();
        if (normalizedKey.isEmpty()) {
            log.warn("GEMINI_API_KEY not configured. AI explanation feature will be disabled.");
            return Client.builder().apiKey("").build();
        }
        return Client.builder().apiKey(normalizedKey).build();
    }
}
