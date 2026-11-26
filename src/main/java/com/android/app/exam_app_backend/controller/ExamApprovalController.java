package com.android.app.exam_app_backend.controller;

import com.android.app.exam_app_backend.common.dto.ApiResponse;
import com.android.app.exam_app_backend.payload.ExamApprovalRequest;
import com.android.app.exam_app_backend.payload.ExamApprovalResponse;
import com.android.app.exam_app_backend.payload.ExamIntegrityVerifyResponse;
import com.android.app.exam_app_backend.payload.ExamResponse;
import com.android.app.exam_app_backend.payload.ExamReviewDetailResponse;
import com.android.app.exam_app_backend.service.ExamApprovalService;
import com.android.app.exam_app_backend.service.ExamIntegrityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/approvals")
public class ExamApprovalController {

    private final ExamApprovalService examApprovalService;
    private final ExamIntegrityService examIntegrityService;

    public ExamApprovalController(ExamApprovalService examApprovalService,
                                  ExamIntegrityService examIntegrityService) {
        this.examApprovalService = examApprovalService;
        this.examIntegrityService = examIntegrityService;
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<ExamResponse>>> getPendingExams(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.<List<ExamResponse>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Pending exams retrieved")
                .data(examApprovalService.getPendingExams(authentication))
                .build());
    }

    @GetMapping("/exams/{examId}")
    public ResponseEntity<ApiResponse<ExamReviewDetailResponse>> getExamForReview(@PathVariable Long examId,
                                                                                  Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.<ExamReviewDetailResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Exam review details retrieved")
                .data(examApprovalService.getExamForReview(examId, authentication))
                .build());
    }

    @GetMapping("/exams/{examId}/history")
    public ResponseEntity<ApiResponse<List<ExamApprovalResponse>>> getApprovalHistory(@PathVariable Long examId,
                                                                                      Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.<List<ExamApprovalResponse>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Exam approval history retrieved")
                .data(examApprovalService.getApprovalHistory(examId, authentication))
                .build());
    }

    @PostMapping("/exams/{examId}/start-review")
    public ResponseEntity<ApiResponse<ExamApprovalResponse>> startReview(@PathVariable Long examId,
                                                                         @RequestBody(required = false) ExamApprovalRequest request,
                                                                         Authentication authentication) {
        String comment = request != null ? request.resolveComment() : null;
        return ResponseEntity.ok(ApiResponse.<ExamApprovalResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Exam review started")
                .data(examApprovalService.startReview(examId, comment, authentication))
                .build());
    }

    @PostMapping("/exams/{examId}/approve")
    public ResponseEntity<ApiResponse<ExamApprovalResponse>> approveExam(@PathVariable Long examId,
                                                                         @RequestBody(required = false) ExamApprovalRequest request,
                                                                         Authentication authentication) {
        String comment = request != null ? request.resolveComment() : null;
        return ResponseEntity.ok(ApiResponse.<ExamApprovalResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Exam approved")
                .data(examApprovalService.approveExam(examId, comment, authentication))
                .build());
    }

    @PostMapping("/exams/{examId}/reject")
    public ResponseEntity<ApiResponse<ExamApprovalResponse>> rejectExam(@PathVariable Long examId,
                                                                        @RequestBody(required = false) ExamApprovalRequest request,
                                                                        Authentication authentication) {
        String comment = request != null ? request.resolveComment() : null;
        return ResponseEntity.ok(ApiResponse.<ExamApprovalResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Exam rejected")
                .data(examApprovalService.rejectExam(examId, comment, authentication))
                .build());
    }

    @PostMapping("/exams/{examId}/request-changes")
    public ResponseEntity<ApiResponse<ExamApprovalResponse>> requestChanges(@PathVariable Long examId,
                                                                            @RequestBody(required = false) ExamApprovalRequest request,
                                                                            Authentication authentication) {
        String comment = request != null ? request.resolveComment() : null;
        return ResponseEntity.ok(ApiResponse.<ExamApprovalResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Exam changes requested")
                .data(examApprovalService.requestChanges(examId, comment, authentication))
                .build());
    }

    @PostMapping("/exams/{examId}/verify-integrity")
    public ResponseEntity<ApiResponse<ExamIntegrityVerifyResponse>> verifyIntegrity(@PathVariable Long examId,
                                                                                    Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.<ExamIntegrityVerifyResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Exam integrity verified")
                .data(examIntegrityService.verifyExamIntegrity(examId, authentication))
                .build());
    }
}
