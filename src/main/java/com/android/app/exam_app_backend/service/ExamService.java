package com.android.app.exam_app_backend.service;

import com.android.app.exam_app_backend.entity.Exam;
import com.android.app.exam_app_backend.entity.ExamResult;
import com.android.app.exam_app_backend.entity.User;
import com.android.app.exam_app_backend.entity.enums.AuditResult;
import com.android.app.exam_app_backend.entity.enums.ExamResultStatus;
import com.android.app.exam_app_backend.exception.ResourceNotFoundException;
import com.android.app.exam_app_backend.payload.ExamCreateRequest;
import com.android.app.exam_app_backend.payload.ExamGenerateRequest;
import com.android.app.exam_app_backend.payload.ExamResponse;
import com.android.app.exam_app_backend.payload.ExamSubmitRequest;
import com.android.app.exam_app_backend.repository.ExamRepository;
import com.android.app.exam_app_backend.repository.ExamResultRepository;
import com.android.app.exam_app_backend.repository.UserRepository;
import com.android.app.exam_app_backend.security.PermissionConstants;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ExamService {

    private final ExamRepository examRepository;
    private final ExamResultRepository examResultRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public ExamService(ExamRepository examRepository,
                       ExamResultRepository examResultRepository,
                       UserRepository userRepository,
                       AuditLogService auditLogService) {
        this.examRepository = examRepository;
        this.examResultRepository = examResultRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<ExamResponse> getExams() {
        return examRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, T(com.android.app.exam_app_backend.security.PermissionConstants).EXAM_CREATE)")
    public ExamResponse createExam(ExamCreateRequest request, Authentication authentication) {
        User creator = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));

        Exam exam = new Exam();
        exam.setCode(generateCode());
        exam.setTitle(request.getTitle());
        exam.setDurationMinutes(request.getDurationMinutes());
        exam.setScorePerQuestion(request.getScorePerQuestion());
        exam.setStartTime(request.getStartTime());
        exam.setEndTime(request.getEndTime());
        exam.setShuffleQuestions(request.getShuffleQuestions());
        exam.setShuffleAnswers(request.getShuffleAnswers());
        exam.setCreatedBy(creator);

        Exam saved = examRepository.save(exam);
        auditLogService.log(creator, PermissionConstants.EXAM_CREATE, "exam", saved.getId(), AuditResult.allow, "Exam created");
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, T(com.android.app.exam_app_backend.security.PermissionConstants).EXAM_GENERATE)")
    public ExamResponse generateExam(ExamGenerateRequest request, Authentication authentication) {
        ExamCreateRequest createRequest = new ExamCreateRequest();
        createRequest.setTitle(request.getTitle());
        createRequest.setDurationMinutes(request.getDurationMinutes());
        createRequest.setScorePerQuestion(request.getScorePerQuestion());
        createRequest.setShuffleQuestions(true);
        createRequest.setShuffleAnswers(true);
        createRequest.setStartTime(LocalDateTime.now());
        createRequest.setEndTime(LocalDateTime.now().plusMinutes(request.getDurationMinutes()));
        return createExam(createRequest, authentication);
    }

    @Transactional
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, T(com.android.app.exam_app_backend.security.PermissionConstants).EXAM_SUBMIT)")
    public String submitExam(Long examId, ExamSubmitRequest request, Authentication authentication) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examId));
        User student = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));

        ExamResult result = examResultRepository.findByStudentIdAndExamId(student.getId(), examId)
                .orElseGet(() -> {
                    ExamResult created = new ExamResult();
                    created.setExam(exam);
                    created.setStudent(student);
                    created.setStartedAt(LocalDateTime.now());
                    return created;
                });

        result.setStatus(ExamResultStatus.SUBMITTED);
        result.setSubmittedAt(LocalDateTime.now());
        examResultRepository.save(result);
        auditLogService.log(student, PermissionConstants.EXAM_SUBMIT, "exam", exam.getId(), AuditResult.allow, "Exam submitted");
        return "Exam submitted: " + examId;
    }

    private String generateCode() {
        return "EX-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 8)
                .toUpperCase(Locale.ROOT);
    }

    private ExamResponse toResponse(Exam exam) {
        return ExamResponse.builder()
                .id(exam.getId())
                .code(exam.getCode())
                .title(exam.getTitle())
                .build();
    }
}
