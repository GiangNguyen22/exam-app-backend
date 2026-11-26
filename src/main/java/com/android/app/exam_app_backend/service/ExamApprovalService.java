package com.android.app.exam_app_backend.service;

import com.android.app.exam_app_backend.entity.Answer;
import com.android.app.exam_app_backend.entity.Exam;
import com.android.app.exam_app_backend.entity.ExamApproval;
import com.android.app.exam_app_backend.entity.ExamQuestion;
import com.android.app.exam_app_backend.entity.ExamVersion;
import com.android.app.exam_app_backend.entity.StudentGroup;
import com.android.app.exam_app_backend.entity.User;
import com.android.app.exam_app_backend.entity.enums.ApprovalDecision;
import com.android.app.exam_app_backend.entity.enums.AuditResult;
import com.android.app.exam_app_backend.entity.enums.ExamStatus;
import com.android.app.exam_app_backend.exception.MakerCheckerViolationException;
import com.android.app.exam_app_backend.exception.ResourceNotFoundException;
import com.android.app.exam_app_backend.payload.ExamApprovalResponse;
import com.android.app.exam_app_backend.payload.ExamQuestionResponse;
import com.android.app.exam_app_backend.payload.ExamResponse;
import com.android.app.exam_app_backend.payload.ExamReviewDetailResponse;
import com.android.app.exam_app_backend.payload.ExamVersionResponse;
import com.android.app.exam_app_backend.repository.ExamApprovalRepository;
import com.android.app.exam_app_backend.repository.ExamQuestionRepository;
import com.android.app.exam_app_backend.repository.ExamRepository;
import com.android.app.exam_app_backend.repository.ExamVersionRepository;
import com.android.app.exam_app_backend.repository.UserRepository;
import com.android.app.exam_app_backend.security.PermissionConstants;
import com.android.app.exam_app_backend.security.annotation.RequireAnyPermission;
import com.android.app.exam_app_backend.security.annotation.RequirePermission;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExamApprovalService {

    private static final int MIN_REJECTION_COMMENT_LENGTH = 10;

    private final ExamRepository examRepository;
    private final ExamVersionRepository examVersionRepository;
    private final ExamApprovalRepository examApprovalRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final UserRepository userRepository;
    private final ExamIntegrityService examIntegrityService;
    private final AuditLogService auditLogService;

    public ExamApprovalService(ExamRepository examRepository,
                               ExamVersionRepository examVersionRepository,
                               ExamApprovalRepository examApprovalRepository,
                               ExamQuestionRepository examQuestionRepository,
                               UserRepository userRepository,
                               ExamIntegrityService examIntegrityService,
                               AuditLogService auditLogService) {
        this.examRepository = examRepository;
        this.examVersionRepository = examVersionRepository;
        this.examApprovalRepository = examApprovalRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.userRepository = userRepository;
        this.examIntegrityService = examIntegrityService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    @RequirePermission(PermissionConstants.EXAM_REVIEW)
    public List<ExamResponse> getPendingExams(Authentication authentication) {
        return examRepository.findByCurrentVersionStatusIn(List.of(ExamStatus.SUBMITTED, ExamStatus.IN_REVIEW))
                .stream()
                .map(this::toExamResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @RequirePermission(PermissionConstants.EXAM_REVIEW)
    public ExamReviewDetailResponse getExamForReview(Long examId, Authentication authentication) {
        Exam exam = findExamOrThrow(examId);
        User reviewer = findUserOrThrow(authentication.getName());
        ExamVersion currentVersion = exam.getCurrentVersion();
        if (currentVersion == null || currentVersion.getStatus() == ExamStatus.DRAFT) {
            boolean isAdmin = hasRole(authentication, "ROLE_ADMIN");
            boolean isOwner = exam.getCreatedBy() != null && exam.getCreatedBy().getId().equals(reviewer.getId());
            if (!isAdmin && !isOwner) {
                auditLogService.logSecurityAlert(
                        reviewer.getId(),
                        AuditLogService.ACTION_UNAUTHORIZED_ACCESS,
                        "exam",
                        examId,
                        AuditResult.deny,
                        "Reviewer attempted to inspect DRAFT exam " + examId
                );
                throw new AccessDeniedException("Đề thi chưa được gửi duyệt, không thể xem trước.");
            }
        }

        List<ExamQuestionResponse> questions = examQuestionRepository.findByExamIdOrderByOrderIndexAsc(examId)
                .stream()
                .map(this::toQuestionResponseForReview)
                .collect(Collectors.toList());

        List<ExamVersionResponse> versions = examVersionRepository.findByExamIdOrderByVersionNoDesc(examId)
                .stream()
                .map(this::toVersionResponse)
                .collect(Collectors.toList());

        List<ExamApprovalResponse> approvals = examApprovalRepository.findByExamIdWithDetails(examId)
                .stream()
                .map(this::toApprovalResponse)
                .collect(Collectors.toList());

        boolean signed = currentVersion != null
                && currentVersion.getIntegrityTag() != null
                && !currentVersion.getIntegrityTag().isBlank();

        return ExamReviewDetailResponse.builder()
                .exam(toExamResponse(exam))
                .questions(questions)
                .versions(versions)
                .approvals(approvals)
                .integritySigned(signed)
                .build();
    }

    @Transactional(readOnly = true)
    @RequireAnyPermission({PermissionConstants.EXAM_REVIEW, PermissionConstants.EXAM_APPROVE, PermissionConstants.EXAM_REJECT, PermissionConstants.EXAM_CREATE})
    public List<ExamApprovalResponse> getApprovalHistory(Long examId, Authentication authentication) {
        Exam exam = findExamOrThrow(examId);
        User user = findUserOrThrow(authentication.getName());

        boolean canReviewAll = authentication.getAuthorities().stream().anyMatch(a -> {
            String auth = a.getAuthority();
            return "ROLE_ADMIN".equals(auth)
                    || PermissionConstants.EXAM_REVIEW.equals(auth)
                    || PermissionConstants.EXAM_APPROVE.equals(auth)
                    || PermissionConstants.EXAM_REJECT.equals(auth);
        });
        boolean isOwner = exam.getCreatedBy() != null && exam.getCreatedBy().getId().equals(user.getId());
        if (!canReviewAll && !isOwner) {
            auditLogService.logSecurityAlert(
                    user.getId(),
                    AuditLogService.ACTION_UNAUTHORIZED_ACCESS,
                    "exam",
                    examId,
                    AuditResult.deny,
                    "Unauthorized access to approval history for exam " + examId
            );
            throw new AccessDeniedException("Bạn không có quyền xem lịch sử phê duyệt của đề thi này.");
        }

        return examApprovalRepository.findByExamIdWithDetails(examId).stream()
                .map(this::toApprovalResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @RequirePermission(PermissionConstants.EXAM_REVIEW)
    public ExamApprovalResponse startReview(Long examId, String comment, Authentication authentication) {
        Exam exam = findExamOrThrow(examId);
        User reviewer = findUserOrThrow(authentication.getName());
        verifyMakerChecker(exam, reviewer, AuditLogService.ACTION_START_REVIEW);

        ExamVersion currentVersion = exam.getCurrentVersion();
        if (currentVersion == null || currentVersion.getStatus() != ExamStatus.SUBMITTED) {
            throw new IllegalStateException("Chỉ có thể bắt đầu rà soát khi đề thi đang ở trạng thái SUBMITTED.");
        }

        currentVersion.setStatus(ExamStatus.IN_REVIEW);
        if (comment != null && !comment.isBlank()) {
            currentVersion.setNotes(comment.trim());
        }
        examVersionRepository.save(currentVersion);

        ExamApproval approval = saveApprovalRecord(
                currentVersion,
                reviewer,
                exam.getCreatedBy().getId(),
                ApprovalDecision.IN_REVIEW,
                comment != null && !comment.isBlank() ? comment.trim() : "Bắt đầu rà soát đề thi"
        );

        auditLogService.log(
                reviewer,
                AuditLogService.ACTION_START_REVIEW,
                "exam",
                exam.getId(),
                AuditResult.allow,
                "Started review for exam v" + currentVersion.getVersionNo()
        );

        return toApprovalResponse(approval);
    }

    @Transactional
    @RequirePermission(PermissionConstants.EXAM_APPROVE)
    public ExamApprovalResponse approveExam(Long examId, String comment, Authentication authentication) {
        Exam exam = findExamOrThrow(examId);
        User approver = findUserOrThrow(authentication.getName());
        verifyMakerChecker(exam, approver, AuditLogService.ACTION_APPROVE_EXAM);

        ExamVersion currentVersion = requireReviewableVersion(exam);
        if (examQuestionRepository.findByExamIdOrderByOrderIndexAsc(examId).isEmpty()) {
            throw new IllegalStateException("Không thể phê duyệt đề thi chưa có câu hỏi.");
        }

        examIntegrityService.signExamVersion(exam, currentVersion);
        currentVersion.setStatus(ExamStatus.APPROVED);
        currentVersion.setNotes(comment != null && !comment.isBlank() ? comment.trim() : "Đã phê duyệt");
        examVersionRepository.save(currentVersion);

        ExamApproval approval = saveApprovalRecord(
                currentVersion,
                approver,
                exam.getCreatedBy().getId(),
                ApprovalDecision.APPROVED,
                comment != null && !comment.isBlank() ? comment.trim() : "Phê duyệt đề thi"
        );

        auditLogService.log(
                approver,
                AuditLogService.ACTION_APPROVE_EXAM,
                "exam",
                exam.getId(),
                AuditResult.allow,
                "Approved exam v" + currentVersion.getVersionNo() + " with HMAC integrity signature"
        );

        return toApprovalResponse(approval);
    }

    @Transactional
    @RequirePermission(PermissionConstants.EXAM_REJECT)
    public ExamApprovalResponse rejectExam(Long examId, String comment, Authentication authentication) {
        Exam exam = findExamOrThrow(examId);
        User approver = findUserOrThrow(authentication.getName());
        verifyMakerChecker(exam, approver, AuditLogService.ACTION_REJECT_EXAM);

        String validatedComment = validateMandatoryComment(comment, "từ chối");
        ExamVersion currentVersion = requireReviewableVersion(exam);

        currentVersion.setStatus(ExamStatus.REJECTED);
        currentVersion.setNotes(validatedComment);
        examVersionRepository.save(currentVersion);

        ExamApproval approval = saveApprovalRecord(
                currentVersion,
                approver,
                exam.getCreatedBy().getId(),
                ApprovalDecision.REJECTED,
                validatedComment
        );

        auditLogService.log(
                approver,
                AuditLogService.ACTION_REJECT_EXAM,
                "exam",
                exam.getId(),
                AuditResult.allow,
                "Rejected exam v" + currentVersion.getVersionNo()
        );

        return toApprovalResponse(approval);
    }

    @Transactional
    @RequireAnyPermission({PermissionConstants.EXAM_REJECT, PermissionConstants.EXAM_REVIEW})
    public ExamApprovalResponse requestChanges(Long examId, String comment, Authentication authentication) {
        Exam savedExam = executeRequestChanges(examId, comment, authentication);
        ExamVersion previousVersion = examVersionRepository
                .findByExamIdAndVersionNo(savedExam.getId(), savedExam.getCurrentVersion().getVersionNo() - 1)
                .orElse(savedExam.getCurrentVersion());

        List<ExamApproval> approvals = examApprovalRepository.findByExamVersionIdOrderByDecidedAtDesc(previousVersion.getId());
        if (!approvals.isEmpty()) {
            return toApprovalResponse(approvals.get(0));
        }
        return ExamApprovalResponse.builder()
                .examId(savedExam.getId())
                .examVersionId(savedExam.getCurrentVersion().getId())
                .versionNo(savedExam.getCurrentVersion().getVersionNo())
                .decision(ApprovalDecision.CHANGES_REQUESTED.name())
                .examStatus(savedExam.getStatus().name())
                .build();
    }

    @Transactional
    @RequireAnyPermission({PermissionConstants.EXAM_REJECT, PermissionConstants.EXAM_REVIEW})
    public ExamResponse requestChangesForExamResponse(Long examId, String comment, Authentication authentication) {
        Exam savedExam = executeRequestChanges(examId, comment, authentication);
        return toExamResponse(savedExam);
    }

    private Exam executeRequestChanges(Long examId, String comment, Authentication authentication) {
        Exam exam = findExamOrThrow(examId);
        User reviewer = findUserOrThrow(authentication.getName());
        verifyMakerChecker(exam, reviewer, AuditLogService.ACTION_REQUEST_CHANGES_EXAM);

        String validatedComment = validateMandatoryComment(comment, "yêu cầu chỉnh sửa");
        ExamVersion currentVersion = requireReviewableVersion(exam);

        currentVersion.setStatus(ExamStatus.CHANGES_REQUESTED);
        currentVersion.setNotes(validatedComment);
        examVersionRepository.save(currentVersion);

        saveApprovalRecord(
                currentVersion,
                reviewer,
                exam.getCreatedBy().getId(),
                ApprovalDecision.CHANGES_REQUESTED,
                validatedComment
        );

        ExamVersion nextVersion = ExamVersion.builder()
                .exam(exam)
                .versionNo(currentVersion.getVersionNo() + 1)
                .status(ExamStatus.DRAFT)
                .notes("Phiên bản mới sau khi có yêu cầu sửa đổi")
                .build();
        nextVersion = examVersionRepository.save(nextVersion);
        exam.setCurrentVersion(nextVersion);
        Exam savedExam = examRepository.save(exam);

        auditLogService.log(
                reviewer,
                AuditLogService.ACTION_REQUEST_CHANGES_EXAM,
                "exam",
                exam.getId(),
                AuditResult.allow,
                "Requested changes for exam v" + currentVersion.getVersionNo() + ", created v" + nextVersion.getVersionNo()
        );

        return savedExam;
    }

    private void verifyMakerChecker(Exam exam, User actor, String attemptedAction) {
        if (exam.getCreatedBy() == null || exam.getCreatedBy().getId() == null) {
            throw new IllegalStateException("Không xác định được người tạo đề thi để kiểm tra Maker-Checker.");
        }
        if (exam.getCreatedBy().getId().equals(actor.getId())) {
            auditLogService.logSecurityAlert(
                    actor.getId(),
                    AuditLogService.ACTION_MAKER_CHECKER_VIOLATION,
                    "exam",
                    exam.getId(),
                    AuditResult.deny,
                    "Maker-Checker violation on " + attemptedAction + ": user " + actor.getUsername() + " attempted to approve/reject own exam " + exam.getId()
            );
            throw new MakerCheckerViolationException("Người tạo đề không được quyền phê duyệt đề thi của chính mình.");
        }
    }

    private ExamVersion requireReviewableVersion(Exam exam) {
        ExamVersion currentVersion = exam.getCurrentVersion();
        if (currentVersion == null
                || (currentVersion.getStatus() != ExamStatus.SUBMITTED && currentVersion.getStatus() != ExamStatus.IN_REVIEW)) {
            throw new IllegalStateException("Thao tác chỉ hợp lệ khi đề thi đang ở trạng thái SUBMITTED hoặc IN_REVIEW.");
        }
        return currentVersion;
    }

    private String validateMandatoryComment(String comment, String actionLabel) {
        if (comment == null || comment.trim().length() < MIN_REJECTION_COMMENT_LENGTH) {
            throw new IllegalArgumentException(
                    "Nhận xét (comment) là bắt buộc và phải có tối thiểu " + MIN_REJECTION_COMMENT_LENGTH + " ký tự khi " + actionLabel + "."
            );
        }
        return comment.trim();
    }

    private ExamApproval saveApprovalRecord(ExamVersion version,
                                            User approver,
                                            Long creatorId,
                                            ApprovalDecision decision,
                                            String comment) {
        ExamApproval approval = ExamApproval.builder()
                .examVersion(version)
                .approver(approver)
                .creatorId(creatorId)
                .decision(decision)
                .comment(comment)
                .decidedAt(LocalDateTime.now())
                .build();
        return examApprovalRepository.save(approval);
    }

    private Exam findExamOrThrow(Long examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examId));
    }

    private User findUserOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private boolean hasRole(Authentication authentication, String roleAuthority) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> roleAuthority.equals(a.getAuthority()));
    }

    private ExamApprovalResponse toApprovalResponse(ExamApproval approval) {
        ExamVersion version = approval.getExamVersion();
        User approver = approval.getApprover();
        return ExamApprovalResponse.builder()
                .id(approval.getId())
                .examId(version.getExam().getId())
                .examVersionId(version.getId())
                .versionNo(version.getVersionNo())
                .approverId(approver.getId())
                .approverUsername(approver.getUsername())
                .approverFullName(approver.getFullName())
                .creatorId(approval.getCreatorId())
                .decision(approval.getDecision().name())
                .comment(approval.getComment())
                .decidedAt(approval.getDecidedAt())
                .integrityTag(version.getIntegrityTag())
                .examStatus(version.getStatus() != null ? version.getStatus().name() : null)
                .build();
    }

    private ExamResponse toExamResponse(Exam exam) {
        List<Long> groupIds = exam.getGroups() == null ? null :
                exam.getGroups().stream().map(StudentGroup::getId).collect(Collectors.toList());
        return ExamResponse.builder()
                .id(exam.getId())
                .code(exam.getCode())
                .title(exam.getTitle())
                .durationMinutes(exam.getDurationMinutes())
                .scorePerQuestion(exam.getScorePerQuestion())
                .startTime(exam.getStartTime())
                .endTime(exam.getEndTime())
                .maxAttempts(exam.getMaxAttempts() == null ? 1 : exam.getMaxAttempts())
                .shuffleQuestions(exam.getShuffleQuestions())
                .shuffleAnswers(exam.getShuffleAnswers())
                .showAnswersAfterSubmit(exam.getShowAnswersAfterSubmit())
                .status(exam.getStatus() != null ? exam.getStatus().name() : ExamStatus.DRAFT.name())
                .versionNo(exam.getCurrentVersion() != null ? exam.getCurrentVersion().getVersionNo() : 1)
                .groupIds(groupIds)
                .totalQuestions(exam.getExamQuestions() != null ? exam.getExamQuestions().size() : 0)
                .build();
    }

    private ExamVersionResponse toVersionResponse(ExamVersion v) {
        return ExamVersionResponse.builder()
                .id(v.getId())
                .examId(v.getExam().getId())
                .versionNo(v.getVersionNo())
                .status(v.getStatus() != null ? v.getStatus().name() : null)
                .notes(v.getNotes())
                .integrityTag(v.getIntegrityTag())
                .createdAt(v.getCreatedAt())
                .build();
    }

    private ExamQuestionResponse toQuestionResponseForReview(ExamQuestion examQuestion) {
        return ExamQuestionResponse.builder()
                .examQuestionId(examQuestion.getId())
                .questionId(examQuestion.getQuestion().getId())
                .orderIndex(examQuestion.getOrderIndex())
                .score(examQuestion.getScore())
                .subjectId(examQuestion.getQuestion().getSubject().getId())
                .topicId(examQuestion.getQuestion().getTopic() == null ? null : examQuestion.getQuestion().getTopic().getId())
                .content(examQuestion.getQuestion().getContent())
                .imageUrl(examQuestion.getQuestion().getImageUrl())
                .type(examQuestion.getQuestion().getType() == null ? null : examQuestion.getQuestion().getType().name())
                .difficulty(examQuestion.getQuestion().getDifficulty() == null ? null : examQuestion.getQuestion().getDifficulty().name())
                .answers(examQuestion.getQuestion().getAnswers().stream()
                        .sorted(Comparator.comparing(Answer::getId, Comparator.nullsLast(Long::compareTo)))
                        .map(answer -> ExamQuestionResponse.AnswerOptionResponse.builder()
                                .id(answer.getId())
                                .content(answer.getContent())
                                .correct(answer.getIsCorrect())
                                .explanation(answer.getExplanation())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
