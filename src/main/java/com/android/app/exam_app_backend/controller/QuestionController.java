package com.android.app.exam_app_backend.controller;

import com.android.app.exam_app_backend.common.dto.ApiResponse;
import com.android.app.exam_app_backend.payload.QuestionCreateRequest;
import com.android.app.exam_app_backend.payload.QuestionResponse;
import com.android.app.exam_app_backend.service.QuestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<QuestionResponse>> createQuestion(@Valid @RequestBody QuestionCreateRequest request,
                                                                        Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.<QuestionResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Question created")
                .data(questionService.createQuestion(request, authentication))
                .build());
    }
}
