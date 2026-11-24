package com.android.app.exam_app_backend.security;

import com.android.app.exam_app_backend.common.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimiterFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterFilter.class);

    @Value("${app.security.rate-limit.login-per-minute:20}")
    private int maxRequestsPerMinute;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WindowCounter> requestCounts = new ConcurrentHashMap<>();

    private static class WindowCounter {
        final long windowStartMs;
        final AtomicInteger count;

        WindowCounter(long windowStartMs) {
            this.windowStartMs = windowStartMs;
            this.count = new AtomicInteger(1);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Apply rate limiting on /api/auth/login and /api/auth/refresh
        if (path.startsWith("/api/auth/login") || path.startsWith("/api/auth/refresh")) {
            String clientIp = getClientIp(request);
            if (isRateLimited(clientIp)) {
                log.warn("Rate limit exceeded for IP={} path={}", clientIp, path);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");

                ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                        .success(false)
                        .code(HttpStatus.TOO_MANY_REQUESTS.value())
                        .message("Quá nhiều yêu cầu. Vui lòng thử lại sau 1 phút.")
                        .data(null)
                        .build();

                response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private synchronized boolean isRateLimited(String clientIp) {
        long now = System.currentTimeMillis();
        long oneMinuteAgo = now - 60_000L;

        // Cleanup expired entries periodically if map grows
        if (requestCounts.size() > 1000) {
            requestCounts.entrySet().removeIf(e -> e.getValue().windowStartMs < oneMinuteAgo);
        }

        WindowCounter counter = requestCounts.get(clientIp);
        if (counter == null || counter.windowStartMs < oneMinuteAgo) {
            requestCounts.put(clientIp, new WindowCounter(now));
            return false;
        }

        int currentCount = counter.count.incrementAndGet();
        return currentCount > maxRequestsPerMinute;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public void clearCounts() {
        requestCounts.clear();
    }
}
