package com.android.app.exam_app_backend.controller;

import com.android.app.exam_app_backend.common.dto.ApiResponse;
import com.android.app.exam_app_backend.payload.AiExplainRequest;
import com.android.app.exam_app_backend.payload.AiExplainResponse;
import com.android.app.exam_app_backend.service.GeminiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private Logger log = LoggerFactory.getLogger(AiController.class);
    private final GeminiService geminiService;

    public AiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("/explain")
    public ResponseEntity<ApiResponse<AiExplainResponse>> explain(@Valid @RequestBody AiExplainRequest request) {
        AiExplainResponse result = geminiService.explain(request);
        if (result == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.<AiExplainResponse>builder()
                            .success(false)
                            .code(HttpStatus.SERVICE_UNAVAILABLE.value())
                            .message("AI service is not available")
                            .build());
        }
        return ResponseEntity.ok(ApiResponse.<AiExplainResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("AI explanation generated")
                .data(result)
                .build());
    }
}
