package com.android.app.exam_app_backend.service;

import com.android.app.exam_app_backend.entity.ExamResult;
import com.android.app.exam_app_backend.exception.ResourceNotFoundException;
import com.android.app.exam_app_backend.payload.ExamResultResponse;
import com.android.app.exam_app_backend.repository.ExamResultRepository;
import com.android.app.exam_app_backend.security.PermissionConstants;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResultService {

    private final ExamResultRepository examResultRepository;

    public ResultService(ExamResultRepository examResultRepository) {
        this.examResultRepository = examResultRepository;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, T(com.android.app.exam_app_backend.security.PermissionConstants).EXAM_VIEW_RESULTS)")
    public ExamResultResponse viewResult(Long examId) {
        ExamResult result = examResultRepository.findTopByExamIdOrderByIdDesc(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Result not found for exam: " + examId));

        return ExamResultResponse.builder()
                .resultId(result.getId())
                .examId(result.getExam().getId())
                .score(result.getScore())
                .status(result.getStatus())
                .submittedAt(result.getSubmittedAt())
                .build();
    }
}
