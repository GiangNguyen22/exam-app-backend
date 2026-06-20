package com.android.app.exam_app_backend.controller;

import com.android.app.exam_app_backend.common.dto.ApiResponse;
import com.android.app.exam_app_backend.payload.QuestionCreateRequest;
import com.android.app.exam_app_backend.payload.QuestionImportResponse;
import com.android.app.exam_app_backend.payload.QuestionResponse;
import com.android.app.exam_app_backend.service.QuestionService;
import com.android.app.exam_app_backend.service.imports.QuestionExcelImportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;
    private final QuestionExcelImportService questionExcelImportService;

    public QuestionController(QuestionService questionService,
                              QuestionExcelImportService questionExcelImportService) {
        this.questionService = questionService;
        this.questionExcelImportService = questionExcelImportService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<QuestionResponse>>> getQuestions() {
        return ResponseEntity.ok(ApiResponse.<List<QuestionResponse>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Questions retrieved")
                .data(questionService.getQuestions())
                .build());
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

    @PutMapping("/{questionId}")
    public ResponseEntity<ApiResponse<QuestionResponse>> updateQuestion(@PathVariable Long questionId,
                                                                        @Valid @RequestBody QuestionCreateRequest request,
                                                                        Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.<QuestionResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Question updated")
                .data(questionService.updateQuestion(questionId, request, authentication))
                .build());
    }

    @DeleteMapping("/{questionId}")
    public ResponseEntity<ApiResponse<String>> deleteQuestion(@PathVariable Long questionId,
                                                              Authentication authentication) {
        questionService.deleteQuestion(questionId, authentication);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Question deleted")
                .data("Question deleted: " + questionId)
                .build());
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<QuestionImportResponse>> importQuestions(@RequestParam("file") MultipartFile file,
                                                                               Authentication authentication) {
        QuestionImportResponse response = questionExcelImportService.importFromExcel(file, authentication);
        return ResponseEntity.ok(ApiResponse.<QuestionImportResponse>builder()
                .success(response.isSuccess())
                .code(HttpStatus.OK.value())
                .message(response.isSuccess() ? "Questions imported" : "Import failed")
                .data(response)
                .build());
    }
}
