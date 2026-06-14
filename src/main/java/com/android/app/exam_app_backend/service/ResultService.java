package com.android.app.exam_app_backend.service;

import com.android.app.exam_app_backend.entity.Exam;
import com.android.app.exam_app_backend.entity.ExamResult;
import com.android.app.exam_app_backend.entity.enums.ExamResultStatus;
import com.android.app.exam_app_backend.exception.ResourceNotFoundException;
import com.android.app.exam_app_backend.payload.ExamReportResponse;
import com.android.app.exam_app_backend.payload.ExamResultResponse;
import com.android.app.exam_app_backend.repository.ExamRepository;
import com.android.app.exam_app_backend.repository.ExamResultRepository;
import com.android.app.exam_app_backend.security.PermissionConstants;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ResultService {

    private final ExamResultRepository examResultRepository;
    private final ExamRepository examRepository;

    public ResultService(ExamResultRepository examResultRepository, ExamRepository examRepository) {
        this.examResultRepository = examResultRepository;
        this.examRepository = examRepository;
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

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, T(com.android.app.exam_app_backend.security.PermissionConstants).EXAM_VIEW_RESULTS)")
    public ExamReportResponse viewExamReport(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examId));
        List<ExamResult> results = examResultRepository.findByExamIdOrderByIdDesc(examId);

        List<BigDecimal> scores = results.stream()
                .map(ExamResult::getScore)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        BigDecimal averageScore = null;
        if (!scores.isEmpty()) {
            BigDecimal totalScore = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            averageScore = totalScore.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
        }

        return ExamReportResponse.builder()
                .examId(exam.getId())
                .examCode(exam.getCode())
                .examTitle(exam.getTitle())
                .totalResults(results.size())
                .submittedCount((int) results.stream().filter(result -> result.getStatus() == ExamResultStatus.SUBMITTED).count())
                .doingCount((int) results.stream().filter(result -> result.getStatus() == ExamResultStatus.DOING).count())
                .averageScore(averageScore)
                .highestScore(scores.stream().max(Comparator.naturalOrder()).orElse(null))
                .lowestScore(scores.stream().min(Comparator.naturalOrder()).orElse(null))
                .results(results.stream().map(this::toStudentResult).collect(Collectors.toList()))
                .build();
    }

    private ExamReportResponse.StudentResult toStudentResult(ExamResult result) {
        return ExamReportResponse.StudentResult.builder()
                .resultId(result.getId())
                .studentId(result.getStudent().getId())
                .studentName(result.getStudent().getFullName())
                .username(result.getStudent().getUsername())
                .score(result.getScore())
                .status(result.getStatus())
                .startedAt(result.getStartedAt())
                .submittedAt(result.getSubmittedAt())
                .build();
    }
}
