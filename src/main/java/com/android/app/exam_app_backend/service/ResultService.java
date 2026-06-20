package com.android.app.exam_app_backend.service;

import com.android.app.exam_app_backend.entity.Answer;
import com.android.app.exam_app_backend.entity.Exam;
import com.android.app.exam_app_backend.entity.ExamQuestion;
import com.android.app.exam_app_backend.entity.ExamResult;
import com.android.app.exam_app_backend.entity.Question;
import com.android.app.exam_app_backend.entity.StudentResponse;
import com.android.app.exam_app_backend.entity.User;
import com.android.app.exam_app_backend.entity.enums.ExamResultStatus;
import com.android.app.exam_app_backend.exception.ResourceNotFoundException;
import com.android.app.exam_app_backend.payload.ExamReportResponse;
import com.android.app.exam_app_backend.payload.ExamResultDetailResponse;
import com.android.app.exam_app_backend.payload.ExamResultResponse;
import com.android.app.exam_app_backend.repository.ExamQuestionRepository;
import com.android.app.exam_app_backend.repository.ExamRepository;
import com.android.app.exam_app_backend.repository.ExamResultRepository;
import com.android.app.exam_app_backend.repository.StudentResponseRepository;
import com.android.app.exam_app_backend.repository.UserRepository;
import com.android.app.exam_app_backend.security.PermissionConstants;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ResultService {

    private final ExamResultRepository examResultRepository;
    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final StudentResponseRepository studentResponseRepository;
    private final UserRepository userRepository;

    public ResultService(ExamResultRepository examResultRepository,
                         ExamRepository examRepository,
                         ExamQuestionRepository examQuestionRepository,
                         StudentResponseRepository studentResponseRepository,
                         UserRepository userRepository) {
        this.examResultRepository = examResultRepository;
        this.examRepository = examRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.studentResponseRepository = studentResponseRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionEvaluator.hasAnyPermission(authentication, T(com.android.app.exam_app_backend.security.PermissionConstants).EXAM_VIEW_RESULTS, T(com.android.app.exam_app_backend.security.PermissionConstants).EXAM_VIEW_OWN_RESULTS)")
    public ExamResultResponse viewResult(Long examId, Authentication authentication) {
        ExamResult result = resolveResultForViewer(examId, authentication);

        return ExamResultResponse.builder()
                .resultId(result.getId())
                .examId(result.getExam().getId())
                .score(result.getScore())
                .status(result.getStatus())
                .submittedAt(result.getSubmittedAt())
                .build();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionEvaluator.hasAnyPermission(authentication, T(com.android.app.exam_app_backend.security.PermissionConstants).EXAM_VIEW_RESULTS, T(com.android.app.exam_app_backend.security.PermissionConstants).EXAM_VIEW_OWN_RESULTS)")
    public ExamResultDetailResponse viewResultDetail(Long examId, Authentication authentication) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examId));

        ExamResult result = resolveResultForViewer(examId, authentication);

        List<ExamQuestion> examQuestions = examQuestionRepository.findByExamIdOrderByOrderIndexAsc(examId);
        Map<Long, StudentResponse> responsesByQuestionId = studentResponseRepository.findByResultId(result.getId()).stream()
                .filter(response -> response.getQuestion() != null)
                .collect(Collectors.toMap(
                        response -> response.getQuestion().getId(),
                        response -> response,
                        (first, second) -> second));

        int correctCount = 0;
        int blankCount = 0;
        List<ExamResultDetailResponse.QuestionResult> questionResults = new java.util.ArrayList<>();
        for (ExamQuestion examQuestion : examQuestions) {
            Question question = examQuestion.getQuestion();
            StudentResponse response = responsesByQuestionId.get(question.getId());
            String type = question.getType() == null ? null : question.getType().name();

            List<Long> selectedAnswerIds = parseSelectedAnswerIds(response == null ? null : response.getSelectedAnswerIds());
            String fillContent = response == null ? null : response.getFillContent();
            boolean blank = isBlankResponse(type, selectedAnswerIds, fillContent);
            boolean correct = !blank && isCorrectResponse(question, type, selectedAnswerIds, fillContent);

            if (blank) {
                blankCount++;
            } else if (correct) {
                correctCount++;
            }

            questionResults.add(ExamResultDetailResponse.QuestionResult.builder()
                    .questionId(question.getId())
                    .orderIndex(examQuestion.getOrderIndex())
                    .content(question.getContent())
                    .type(type)
                    .correct(correct)
                    .blank(blank)
                    .selectedAnswerIds(selectedAnswerIds)
                    .fillContent(fillContent)
                    .answers(question.getAnswers().stream()
                            .map(answer -> ExamResultDetailResponse.AnswerResult.builder()
                                    .id(answer.getId())
                                    .content(answer.getContent())
                                    .correct(answer.getIsCorrect())
                                    .explanation(answer.getExplanation())
                                    .build())
                            .collect(Collectors.toList()))
                    .build());
        }

        int total = examQuestions.size();
        int wrongCount = total - correctCount - blankCount;

        return ExamResultDetailResponse.builder()
                .resultId(result.getId())
                .examId(exam.getId())
                .examTitle(exam.getTitle())
                .score(result.getScore())
                .status(result.getStatus())
                .submittedAt(result.getSubmittedAt())
                .totalQuestions(total)
                .correctCount(correctCount)
                .wrongCount(wrongCount)
                .blankCount(blankCount)
                .questions(questionResults)
                .build();
    }

    private boolean isStudent(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_STUDENT".equals(authority.getAuthority()));
    }

    private ExamResult resolveResultForViewer(Long examId, Authentication authentication) {
        if (isStudent(authentication)) {
            User student = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));
            return examResultRepository.findByStudentIdAndExamId(student.getId(), examId)
                    .orElseThrow(() -> new ResourceNotFoundException("Result not found for exam: " + examId));
        }

        return examResultRepository.findTopByExamIdOrderByIdDesc(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Result not found for exam: " + examId));
    }

    private List<Long> parseSelectedAnswerIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return java.util.Collections.emptyList();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> {
                    try {
                        return Long.parseLong(value);
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private boolean isBlankResponse(String type, List<Long> selectedAnswerIds, String fillContent) {
        if ("FILL_BLANK".equals(type)) {
            return fillContent == null || fillContent.trim().isEmpty();
        }
        return selectedAnswerIds.isEmpty();
    }

    private boolean isCorrectResponse(Question question, String type, List<Long> selectedAnswerIds, String fillContent) {
        if ("FILL_BLANK".equals(type)) {
            String normalized = fillContent == null ? "" : fillContent.trim().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty()) {
                return false;
            }
            return question.getAnswers().stream()
                    .filter(answer -> Boolean.TRUE.equals(answer.getIsCorrect()))
                    .map(answer -> answer.getContent() == null ? "" : answer.getContent().trim().toLowerCase(Locale.ROOT))
                    .anyMatch(normalized::equals);
        }
        Set<Long> selected = Set.copyOf(selectedAnswerIds);
        Set<Long> correctIds = question.getAnswers().stream()
                .filter(answer -> Boolean.TRUE.equals(answer.getIsCorrect()))
                .map(Answer::getId)
                .collect(Collectors.toSet());
        return !selected.isEmpty() && selected.equals(correctIds);
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
                .studentCode(result.getStudent().getStudentId())
                .studentName(result.getStudent().getFullName())
                .username(result.getStudent().getUsername())
                .score(result.getScore())
                .status(result.getStatus())
                .startedAt(result.getStartedAt())
                .submittedAt(result.getSubmittedAt())
                .build();
    }
}
