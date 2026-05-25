package com.android.app.exam_app_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

@Configuration
public class AuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        // Temporary implementation; returns a constant 'system' auditor.
        // Later, integrate with Spring Security to return the authenticated username.
        return () -> Optional.of("system");
    }
}

