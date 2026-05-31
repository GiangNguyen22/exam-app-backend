package com.android.app.exam_app_backend.controller;

import com.android.app.exam_app_backend.common.dto.ApiResponse;
import com.android.app.exam_app_backend.payload.ExamCreateRequest;
import com.android.app.exam_app_backend.payload.ExamGenerateRequest;
import com.android.app.exam_app_backend.payload.ExamQuestionCreateRequest;
import com.android.app.exam_app_backend.payload.ExamQuestionResponse;
import com.android.app.exam_app_backend.payload.ExamResponse;
import com.android.app.exam_app_backend.payload.ExamSubmitRequest;
import com.android.app.exam_app_backend.service.ExamService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ExamResponse>>> getExams() {
        return ResponseEntity.ok(ApiResponse.<List<ExamResponse>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Exams retrieved")
                .data(examService.getExams())
                .build());
    }

    @GetMapping("/{examId}/questions")
    public ResponseEntity<ApiResponse<List<ExamQuestionResponse>>> getExamQuestions(@PathVariable Long examId) {
        return ResponseEntity.ok(ApiResponse.<List<ExamQuestionResponse>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Exam questions retrieved")
                .data(examService.getExamQuestions(examId))
                .build());
    }

    @PostMapping("/{examId}/questions")
    public ResponseEntity<ApiResponse<ExamQuestionResponse>> createQuestionForExam(@PathVariable Long examId,
                                                                                   @Valid @RequestBody ExamQuestionCreateRequest request,
                                                                                   Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.<ExamQuestionResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Exam question created")
                .data(examService.createQuestionForExam(examId, request, authentication))
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ExamResponse>> createExam(@Valid @RequestBody ExamCreateRequest request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.<ExamResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Exam created")
                .data(examService.createExam(request, authentication))
                .build());
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<ExamResponse>> generateExam(@Valid @RequestBody ExamGenerateRequest request,
                                                                  Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.<ExamResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Exam generated")
                .data(examService.generateExam(request, authentication))
                .build());
    }

    @PostMapping("/{examId}/submit")
    public ResponseEntity<ApiResponse<String>> submitExam(@PathVariable Long examId,
                                                          @RequestBody ExamSubmitRequest request,
                                                          Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Exam submitted")
                .data(examService.submitExam(examId, request, authentication))
                .build());
    }
}
