package com.android.app.exam_app_backend.service;

import com.android.app.exam_app_backend.entity.Answer;
import com.android.app.exam_app_backend.entity.Exam;
import com.android.app.exam_app_backend.entity.ExamQuestion;
import com.android.app.exam_app_backend.entity.ExamVersion;
import com.android.app.exam_app_backend.entity.Question;
import com.android.app.exam_app_backend.entity.User;
import com.android.app.exam_app_backend.entity.enums.AuditResult;
import com.android.app.exam_app_backend.exception.ExamIntegrityCompromisedException;
import com.android.app.exam_app_backend.exception.ResourceNotFoundException;
import com.android.app.exam_app_backend.payload.ExamIntegrityVerifyResponse;
import com.android.app.exam_app_backend.repository.ExamQuestionRepository;
import com.android.app.exam_app_backend.repository.ExamRepository;
import com.android.app.exam_app_backend.repository.UserRepository;
import com.android.app.exam_app_backend.security.PermissionConstants;
import com.android.app.exam_app_backend.security.crypto.CryptoKeyProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class ExamIntegrityService {

    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final UserRepository userRepository;
    private final CryptoKeyProvider cryptoKeyProvider;
    private final AuditLogService auditLogService;
    private final ObjectMapper canonicalObjectMapper;

    public ExamIntegrityService(ExamRepository examRepository,
                                ExamQuestionRepository examQuestionRepository,
                                UserRepository userRepository,
                                CryptoKeyProvider cryptoKeyProvider,
                                AuditLogService auditLogService) {
        this.examRepository = examRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.userRepository = userRepository;
        this.cryptoKeyProvider = cryptoKeyProvider;
        this.auditLogService = auditLogService;
        this.canonicalObjectMapper = new ObjectMapper();
        this.canonicalObjectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    public String signExamVersion(Exam exam, ExamVersion version) {
        if (exam == null || version == null) {
            throw new IllegalArgumentException("Exam and ExamVersion are required for signing.");
        }
        String canonicalJson = buildCanonicalJson(exam, version);
        String hmac = computeHmacSha256Hex(canonicalJson);
        version.setIntegrityTag(hmac);
        return hmac;
    }

    public void verifyExamVersion(Exam exam, ExamVersion version, User actor) {
        if (exam == null || version == null) {
            throw new IllegalArgumentException("Exam and ExamVersion are required for integrity verification.");
        }
        String storedTag = version.getIntegrityTag();
        if (storedTag == null || storedTag.trim().isEmpty()) {
            logIntegrityMismatch(actor, version, "Missing integrity tag for exam v" + version.getVersionNo());
            throw new ExamIntegrityCompromisedException("Phiên bản đề thi chưa có chữ ký toàn vẹn (integrity_tag).");
        }

        String recomputedTag;
        try {
            String canonicalJson = buildCanonicalJson(exam, version);
            recomputedTag = computeHmacSha256Hex(canonicalJson);
        } catch (RuntimeException ex) {
            if (isDecryptionTamperFailure(ex)) {
                logIntegrityMismatch(actor, version, "Ciphertext decryption failure detected on exam v" + version.getVersionNo());
                throw new ExamIntegrityCompromisedException("Phát hiện dữ liệu đề thi bị sửa đổi hoặc hư hỏng bản mã (HMAC/GCM mismatch).");
            }
            throw ex;
        }

        boolean matches = MessageDigest.isEqual(
                storedTag.trim().getBytes(StandardCharsets.UTF_8),
                recomputedTag.getBytes(StandardCharsets.UTF_8)
        );
        if (!matches) {
            logIntegrityMismatch(actor, version, "HMAC-SHA256 mismatch detected on exam " + exam.getId() + " v" + version.getVersionNo());
            throw new ExamIntegrityCompromisedException("Cảnh báo bảo mật: Dữ liệu đề thi đã bị thay đổi trái phép (vi phạm toàn vẹn HMAC-SHA256).");
        }
    }

    @Transactional(readOnly = true)
    public ExamIntegrityVerifyResponse verifyExamIntegrity(Long examId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examId));
        User actor = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));

        verifyCallerCanInspectIntegrity(exam, actor, authentication);

        ExamVersion currentVersion = exam.getCurrentVersion();
        if (currentVersion == null) {
            throw new IllegalStateException("Đề thi chưa có phiên bản để kiểm tra toàn vẹn.");
        }

        verifyExamVersion(exam, currentVersion, actor);

        auditLogService.logSecurityAlert(
                actor.getId(),
                AuditLogService.ACTION_INTEGRITY_VERIFIED,
                "exam_version",
                currentVersion.getId(),
                AuditResult.allow,
                "Verified HMAC-SHA256 integrity for exam " + exam.getId() + " v" + currentVersion.getVersionNo()
        );

        return ExamIntegrityVerifyResponse.builder()
                .examId(exam.getId())
                .examVersionId(currentVersion.getId())
                .versionNo(currentVersion.getVersionNo())
                .status(currentVersion.getStatus() != null ? currentVersion.getStatus().name() : null)
                .integrityTag(currentVersion.getIntegrityTag())
                .valid(true)
                .verifiedAt(LocalDateTime.now())
                .build();
    }

    public String buildCanonicalJson(Exam exam, ExamVersion version) {
        List<ExamQuestion> examQuestions = new ArrayList<>(examQuestionRepository.findByExamIdOrderByOrderIndexAsc(exam.getId()));
        examQuestions.sort(
                Comparator.comparing(ExamQuestion::getOrderIndex, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ExamQuestion::getId, Comparator.nullsLast(Long::compareTo))
        );

        List<Map<String, Object>> canonicalQuestions = new ArrayList<>();
        for (ExamQuestion eq : examQuestions) {
            Question question = eq.getQuestion();
            List<Answer> sortedAnswers = new ArrayList<>(question.getAnswers());
            sortedAnswers.sort(Comparator.comparing(Answer::getId, Comparator.nullsLast(Long::compareTo)));

            List<Map<String, Object>> canonicalAnswers = new ArrayList<>();
            for (Answer answer : sortedAnswers) {
                Map<String, Object> answerMap = new TreeMap<>();
                answerMap.put("content", normalizeText(answer.getContent()));
                answerMap.put("explanation", normalizeText(answer.getExplanation()));
                answerMap.put("id", answer.getId());
                answerMap.put("isCorrect", Boolean.TRUE.equals(answer.getIsCorrect()));
                canonicalAnswers.add(answerMap);
            }

            Map<String, Object> questionMap = new TreeMap<>();
            questionMap.put("answers", canonicalAnswers);
            questionMap.put("content", normalizeText(question.getContent()));
            questionMap.put("difficulty", question.getDifficulty() != null ? question.getDifficulty().name() : null);
            questionMap.put("imageUrl", normalizeText(question.getImageUrl()));
            questionMap.put("orderIndex", eq.getOrderIndex());
            questionMap.put("questionId", question.getId());
            questionMap.put("score", normalizeDecimal(eq.getScore()));
            questionMap.put("type", question.getType() != null ? question.getType().name() : null);
            canonicalQuestions.add(questionMap);
        }

        Map<String, Object> root = new TreeMap<>();
        root.put("durationMinutes", exam.getDurationMinutes());
        root.put("examCode", normalizeText(exam.getCode()));
        root.put("examId", exam.getId());
        root.put("maxAttempts", exam.getMaxAttempts() == null ? 1 : exam.getMaxAttempts());
        root.put("questions", canonicalQuestions);
        root.put("scorePerQuestion", normalizeDecimal(exam.getScorePerQuestion()));
        root.put("showAnswersAfterSubmit", Boolean.TRUE.equals(exam.getShowAnswersAfterSubmit()));
        root.put("shuffleAnswers", Boolean.TRUE.equals(exam.getShuffleAnswers()));
        root.put("shuffleQuestions", Boolean.TRUE.equals(exam.getShuffleQuestions()));
        root.put("title", normalizeText(exam.getTitle()));
        root.put("versionNo", version.getVersionNo());

        try {
            return canonicalObjectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize canonical exam JSON", e);
        }
    }

    private String computeHmacSha256Hex(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(cryptoKeyProvider.getIntegritySecretKey());
            byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(rawHmac.length * 2);
            for (byte b : rawHmac) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to compute HMAC-SHA256 integrity tag", e);
        }
    }

    private void verifyCallerCanInspectIntegrity(Exam exam, User actor, Authentication authentication) {
        boolean hasPrivilegedAuthority = authentication.getAuthorities().stream()
                .anyMatch(a -> {
                    String auth = a.getAuthority();
                    return "ROLE_ADMIN".equals(auth)
                            || PermissionConstants.EXAM_RELEASE.equals(auth)
                            || PermissionConstants.EXAM_REVIEW.equals(auth)
                            || PermissionConstants.EXAM_APPROVE.equals(auth);
                });
        if (hasPrivilegedAuthority) {
            return;
        }
        boolean isOwner = exam.getCreatedBy() != null && exam.getCreatedBy().getId().equals(actor.getId());
        if (!isOwner) {
            auditLogService.logSecurityAlert(
                    actor.getId(),
                    AuditLogService.ACTION_UNAUTHORIZED_ACCESS,
                    "exam",
                    exam.getId(),
                    AuditResult.deny,
                    "Unauthorized integrity verification attempt on exam " + exam.getId()
            );
            throw new AccessDeniedException("Bạn không có quyền kiểm tra toàn vẹn đề thi này");
        }
    }

    private void logIntegrityMismatch(User actor, ExamVersion version, String reason) {
        auditLogService.logSecurityAlert(
                actor != null ? actor.getId() : null,
                AuditLogService.ACTION_INTEGRITY_MISMATCH,
                "exam_version",
                version != null ? version.getId() : null,
                AuditResult.deny,
                reason
        );
    }

    private boolean isDecryptionTamperFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null && current.getMessage().contains("Decryption failed")) {
                return true;
            }
            if (current instanceof javax.crypto.AEADBadTagException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeDecimal(BigDecimal value) {
        if (value == null) {
            return null;
        }
        BigDecimal normalized = value.stripTrailingZeros();
        return normalized.scale() < 0 ? normalized.setScale(0).toPlainString() : normalized.toPlainString();
    }
}
