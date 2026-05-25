package com.android.app.exam_app_backend.controller;

import com.android.app.exam_app_backend.common.dto.ApiResponse;
import com.android.app.exam_app_backend.payload.ExamResultResponse;
import com.android.app.exam_app_backend.service.ResultService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/results")
public class ResultController {

    private final ResultService resultService;

    public ResultController(ResultService resultService) {
        this.resultService = resultService;
    }

    @GetMapping("/{examId}")
    public ResponseEntity<ApiResponse<ExamResultResponse>> viewResult(@PathVariable Long examId) {
        return ResponseEntity.ok(ApiResponse.<ExamResultResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Result retrieved")
                .data(resultService.viewResult(examId))
                .build());
    }
}
