package com.android.app.exam_app_backend.service;

import com.android.app.exam_app_backend.entity.Answer;
import com.android.app.exam_app_backend.entity.Exam;
import com.android.app.exam_app_backend.entity.ExamQuestion;
import com.android.app.exam_app_backend.entity.ExamResult;
import com.android.app.exam_app_backend.entity.Question;
import com.android.app.exam_app_backend.entity.StudentResponse;
import com.android.app.exam_app_backend.entity.Subject;
import com.android.app.exam_app_backend.entity.Topic;
import com.android.app.exam_app_backend.entity.User;
import com.android.app.exam_app_backend.entity.enums.AuditResult;
import com.android.app.exam_app_backend.entity.enums.Difficulty;
import com.android.app.exam_app_backend.entity.enums.ExamResultStatus;
import com.android.app.exam_app_backend.exception.ResourceNotFoundException;
import com.android.app.exam_app_backend.payload.ExamCreateRequest;
import com.android.app.exam_app_backend.payload.ExamGenerateRequest;
import com.android.app.exam_app_backend.payload.ExamQuestionCreateRequest;
import com.android.app.exam_app_backend.payload.ExamQuestionResponse;
import com.android.app.exam_app_backend.payload.ExamResponse;
import com.android.app.exam_app_backend.payload.ExamSubmitRequest;
import com.android.app.exam_app_backend.payload.ExamUpdateRequest;
import com.android.app.exam_app_backend.repository.AnswerRepository;
import com.android.app.exam_app_backend.repository.ExamQuestionRepository;
import com.android.app.exam_app_backend.repository.ExamRepository;
import com.android.app.exam_app_backend.repository.ExamResultRepository;
import com.android.app.exam_app_backend.repository.QuestionRepository;
import com.android.app.exam_app_backend.repository.StudentResponseRepository;
import com.android.app.exam_app_backend.repository.SubjectRepository;
import com.android.app.exam_app_backend.repository.TopicRepository;
import com.android.app.exam_app_backend.repository.UserRepository;
import com.android.app.exam_app_backend.security.PermissionConstants;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ExamService {

    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamResultRepository examResultRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final StudentResponseRepository studentResponseRepository;
    private final AuditLogService auditLogService;

    public ExamService(ExamRepository examRepository,
                       ExamQuestionRepository examQuestionRepository,
                       ExamResultRepository examResultRepository,
                       QuestionRepository questionRepository,
                       AnswerRepository answerRepository,
                       SubjectRepository subjectRepository,
                       TopicRepository topicRepository,
                       UserRepository userRepository,
                       StudentResponseRepository studentResponseRepository,
                       AuditLogService auditLogService) {
        this.examRepository = examRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.examResultRepository = examResultRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
        this.studentResponseRepository = studentResponseRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<ExamResponse> getExams(Authentication authentication) {
        boolean studentView = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_STUDENT".equals(authority.getAuthority()));
        LocalDateTime now = LocalDateTime.now();

        return examRepository.findAll().stream()
                .filter(exam -> !studentView || isExamVisibleToStudent(exam, now))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private boolean isExamVisibleToStudent(Exam exam, LocalDateTime now) {
        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            return false;
        }
        return exam.getEndTime() == null || now.isBefore(exam.getEndTime());
    }

    private boolean isStudent(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_STUDENT".equals(authority.getAuthority()));
    }

    @Transactional(readOnly = true)
    public List<ExamQuestionResponse> getExamQuestions(Long examId, Authentication authentication) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examId));
        if (isStudent(authentication) && !isExamVisibleToStudent(exam, LocalDateTime.now())) {
            throw new IllegalArgumentException("Exam is no longer available");
        }

        // Học sinh không được nhận đáp án đúng / giải thích trong lúc làm bài.
        boolean includeCorrectness = !isStudent(authentication);
        return examQuestionRepository.findByExamIdOrderByOrderIndexAsc(examId).stream()
                .map(examQuestion -> toQuestionResponse(examQuestion, includeCorrectness))
                .collect(Collectors.toList());
    }

    @Transactional
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, T(com.android.app.exam_app_backend.security.PermissionConstants).QUESTION_CREATE)")
    public ExamQuestionResponse createQuestionForExam(Long examId, ExamQuestionCreateRequest request, Authentication authentication) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examId));
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + request.getSubjectId()));
        Topic topic = null;
        if (request.getTopicId() != null) {
            topic = topicRepository.findById(request.getTopicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + request.getTopicId()));
        }
        User creator = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));

        Question question = new Question();
        question.setSubject(subject);
        question.setTopic(topic);
        question.setContent(request.getContent());
        question.setType(request.getType());
        question.setDifficulty(request.getDifficulty());
        question.setCreatedBy(creator);
        Question savedQuestion = questionRepository.save(question);

        request.getAnswers().forEach(answerRequest -> {
            Answer answer = new Answer();
            answer.setQuestion(savedQuestion);
            answer.setContent(answerRequest.getContent());
            answer.setIsCorrect(Boolean.TRUE.equals(answerRequest.getCorrect()));
            answer.setExplanation(answerRequest.getExplanation());
            answerRepository.save(answer);
            savedQuestion.getAnswers().add(answer);
        });

        ExamQuestion examQuestion = new ExamQuestion();
        examQuestion.setExam(exam);
        examQuestion.setQuestion(savedQuestion);
        examQuestion.setOrderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : nextOrderIndex(examId));
        examQuestion.setScore(request.getScore() != null ? request.getScore() : exam.getScorePerQuestion());
        ExamQuestion savedExamQuestion = examQuestionRepository.save(examQuestion);

        auditLogService.log(creator, PermissionConstants.QUESTION_CREATE, "exam_question", savedExamQuestion.getId(), AuditResult.allow, "Question created for exam");
        return toQuestionResponse(savedExamQuestion);
    }

    @Transactional
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, T(com.android.app.exam_app_backend.security.PermissionConstants).QUESTION_UPDATE)")
    public ExamQuestionResponse updateQuestionForExam(Long examId, Long questionId, ExamQuestionCreateRequest request, Authentication authentication) {
        ExamQuestion examQuestion = examQuestionRepository.findByExamIdAndQuestionId(examId, questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found in exam: " + questionId));
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + request.getSubjectId()));
        Topic topic = null;
        if (request.getTopicId() != null) {
            topic = topicRepository.findById(request.getTopicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + request.getTopicId()));
        }
        User updater = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));

        Question question = examQuestion.getQuestion();
        question.setSubject(subject);
        question.setTopic(topic);
        question.setContent(request.getContent());
        question.setType(request.getType());
        question.setDifficulty(request.getDifficulty());
        question.getAnswers().clear();
        request.getAnswers().forEach(answerRequest -> {
            Answer answer = new Answer();
            answer.setQuestion(question);
            answer.setContent(answerRequest.getContent());
            answer.setIsCorrect(Boolean.TRUE.equals(answerRequest.getCorrect()));
            answer.setExplanation(answerRequest.getExplanation());
            question.getAnswers().add(answer);
        });
        questionRepository.save(question);

        if (request.getOrderIndex() != null) {
            examQuestion.setOrderIndex(request.getOrderIndex());
        }
        if (request.getScore() != null) {
            examQuestion.setScore(request.getScore());
        }
        ExamQuestion savedExamQuestion = examQuestionRepository.save(examQuestion);
        auditLogService.log(updater, PermissionConstants.QUESTION_UPDATE, "exam_question", savedExamQuestion.getId(), AuditResult.allow, "Question updated for exam");
        return toQuestionResponse(savedExamQuestion);
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
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, T(com.android.app.exam_app_backend.security.PermissionConstants).EXAM_CREATE)")
    public ExamResponse updateExam(Long examId, ExamUpdateRequest request, Authentication authentication) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examId));
        User updater = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));

        exam.setTitle(request.getTitle());
        exam.setDurationMinutes(request.getDurationMinutes());
        exam.setScorePerQuestion(request.getScorePerQuestion());
        exam.setStartTime(request.getStartTime());
        exam.setEndTime(request.getEndTime());
        exam.setShuffleQuestions(request.getShuffleQuestions());
        exam.setShuffleAnswers(request.getShuffleAnswers());

        Exam saved = examRepository.save(exam);
        auditLogService.log(updater, PermissionConstants.EXAM_CREATE, "exam", saved.getId(), AuditResult.allow, "Exam updated");
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, T(com.android.app.exam_app_backend.security.PermissionConstants).EXAM_GENERATE)")
    public ExamResponse generateExam(ExamGenerateRequest request, Authentication authentication) {
        User creator = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));
        if (request.getSubjectId() == null) {
            throw new IllegalArgumentException("Subject is required");
        }
        subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + request.getSubjectId()));
        if (request.getTopicId() != null) {
            topicRepository.findById(request.getTopicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + request.getTopicId()));
        }

        List<Question> selectedQuestions = new ArrayList<>();
        selectedQuestions.addAll(selectQuestions(request.getSubjectId(), request.getTopicId(), Difficulty.EASY, request.getEasyCount()));
        selectedQuestions.addAll(selectQuestions(request.getSubjectId(), request.getTopicId(), Difficulty.MEDIUM, request.getMediumCount()));
        selectedQuestions.addAll(selectQuestions(request.getSubjectId(), request.getTopicId(), Difficulty.HARD, request.getHardCount()));
        if (selectedQuestions.isEmpty()) {
            throw new IllegalArgumentException("Select at least one question");
        }
        Collections.shuffle(selectedQuestions);

        Exam exam = new Exam();
        exam.setCode(generateCode());
        exam.setTitle(request.getTitle());
        exam.setDurationMinutes(request.getDurationMinutes());
        exam.setScorePerQuestion(request.getScorePerQuestion());
        exam.setShuffleQuestions(true);
        exam.setShuffleAnswers(true);
        exam.setStartTime(LocalDateTime.now());
        exam.setEndTime(LocalDateTime.now().plusMinutes(request.getDurationMinutes()));
        exam.setCreatedBy(creator);

        Exam savedExam = examRepository.save(exam);
        int orderIndex = 1;
        for (Question question : selectedQuestions) {
            ExamQuestion examQuestion = new ExamQuestion();
            examQuestion.setExam(savedExam);
            examQuestion.setQuestion(question);
            examQuestion.setOrderIndex(orderIndex++);
            examQuestion.setScore(savedExam.getScorePerQuestion());
            examQuestionRepository.save(examQuestion);
        }
        auditLogService.log(creator, PermissionConstants.EXAM_GENERATE, "exam", savedExam.getId(), AuditResult.allow, "Exam generated");
        return toResponse(savedExam);
    }

    private List<Question> selectQuestions(Long subjectId, Long topicId, Difficulty difficulty, Integer count) {
        int requestedCount = count == null ? 0 : count;
        if (requestedCount <= 0) {
            return new ArrayList<>();
        }
        List<Question> candidates = topicId == null
                ? questionRepository.findBySubjectIdAndDifficulty(subjectId, difficulty)
                : questionRepository.findBySubjectIdAndTopicIdAndDifficulty(subjectId, topicId, difficulty);
        Collections.shuffle(candidates);
        if (candidates.size() < requestedCount) {
            throw new IllegalArgumentException("Not enough " + difficulty.name().toLowerCase(Locale.ROOT) + " questions");
        }
        return new ArrayList<>(candidates.subList(0, requestedCount));
    }

    @Transactional
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, T(com.android.app.exam_app_backend.security.PermissionConstants).EXAM_SUBMIT)")
    public String submitExam(Long examId, ExamSubmitRequest request, Authentication authentication) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examId));
        if (isStudent(authentication) && !isExamVisibleToStudent(exam, LocalDateTime.now())) {
            throw new IllegalArgumentException("Exam is no longer available");
        }
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
        result = examResultRepository.save(result);
        saveStudentResponses(result, request);
        result.setScore(calculateScore(examId, request));
        examResultRepository.save(result);
        auditLogService.log(student, PermissionConstants.EXAM_SUBMIT, "exam", exam.getId(), AuditResult.allow, "Exam submitted");
        return "Exam submitted: " + examId;
    }

    private void saveStudentResponses(ExamResult result, ExamSubmitRequest request) {
        studentResponseRepository.deleteByResultId(result.getId());
        Map<Long, Question> examQuestionMap = examQuestionRepository.findByExamIdOrderByOrderIndexAsc(result.getExam().getId()).stream()
                .collect(Collectors.toMap(examQuestion -> examQuestion.getQuestion().getId(), ExamQuestion::getQuestion));

        if (request.getAnswers() == null) {
            return;
        }

        List<StudentResponse> responses = request.getAnswers().stream()
                .filter(answer -> answer.getQuestionId() != null)
                .filter(answer -> examQuestionMap.containsKey(answer.getQuestionId()))
                .map(answer -> {
                    StudentResponse response = new StudentResponse();
                    response.setResult(result);
                    response.setQuestion(examQuestionMap.get(answer.getQuestionId()));
                    response.setSelectedAnswerIds(toAnswerIdString(answer.getSelectedAnswerIds()));
                    response.setFillContent(answer.getFillContent());
                    response.setLastSavedAt(LocalDateTime.now());
                    return response;
                })
                .collect(Collectors.toList());
        studentResponseRepository.saveAll(responses);
    }

    private BigDecimal calculateScore(Long examId, ExamSubmitRequest request) {
        Map<Long, ExamSubmitRequest.StudentAnswerRequest> submittedAnswers = request.getAnswers() == null
                ? Collections.emptyMap()
                : request.getAnswers().stream()
                .filter(answer -> answer.getQuestionId() != null)
                .collect(Collectors.toMap(
                        ExamSubmitRequest.StudentAnswerRequest::getQuestionId,
                        answer -> answer,
                        (first, second) -> second
                ));

        BigDecimal totalScore = BigDecimal.ZERO;
        for (ExamQuestion examQuestion : examQuestionRepository.findByExamIdOrderByOrderIndexAsc(examId)) {
            ExamSubmitRequest.StudentAnswerRequest submittedAnswer = submittedAnswers.get(examQuestion.getQuestion().getId());
            if (submittedAnswer == null) {
                continue;
            }
            if (isCorrectAnswer(examQuestion.getQuestion(), submittedAnswer)) {
                totalScore = totalScore.add(examQuestion.getScore() == null ? BigDecimal.ZERO : examQuestion.getScore());
            }
        }
        return totalScore;
    }

    private boolean isCorrectAnswer(Question question, ExamSubmitRequest.StudentAnswerRequest submittedAnswer) {
        if (question.getType() != null && "FILL_BLANK".equals(question.getType().name())) {
            String fillContent = normalizeText(submittedAnswer.getFillContent());
            if (fillContent.isEmpty()) {
                return false;
            }
            return question.getAnswers().stream()
                    .filter(answer -> Boolean.TRUE.equals(answer.getIsCorrect()))
                    .map(answer -> normalizeText(answer.getContent()))
                    .anyMatch(fillContent::equals);
        }

        Set<Long> selectedAnswerIds = submittedAnswer.getSelectedAnswerIds() == null
                ? Collections.emptySet()
                : submittedAnswer.getSelectedAnswerIds().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (selectedAnswerIds.isEmpty()) {
            return false;
        }

        Set<Long> correctAnswerIds = question.getAnswers().stream()
                .filter(answer -> Boolean.TRUE.equals(answer.getIsCorrect()))
                .map(Answer::getId)
                .collect(Collectors.toSet());
        return selectedAnswerIds.equals(correctAnswerIds);
    }

    private String toAnswerIdString(List<Long> selectedAnswerIds) {
        if (selectedAnswerIds == null || selectedAnswerIds.isEmpty()) {
            return "";
        }
        return selectedAnswerIds.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String generateCode() {
        return "EX-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 8)
                .toUpperCase(Locale.ROOT);
    }

    private Integer nextOrderIndex(Long examId) {
        return examQuestionRepository.findByExamIdOrderByOrderIndexAsc(examId).stream()
                .map(ExamQuestion::getOrderIndex)
                .filter(index -> index != null)
                .max(Integer::compareTo)
                .map(index -> index + 1)
                .orElse(1);
    }

    private ExamResponse toResponse(Exam exam) {
        return ExamResponse.builder()
                .id(exam.getId())
                .code(exam.getCode())
                .title(exam.getTitle())
                .durationMinutes(exam.getDurationMinutes())
                .scorePerQuestion(exam.getScorePerQuestion())
                .startTime(exam.getStartTime())
                .endTime(exam.getEndTime())
                .shuffleQuestions(exam.getShuffleQuestions())
                .shuffleAnswers(exam.getShuffleAnswers())
                .build();
    }

    private ExamQuestionResponse toQuestionResponse(ExamQuestion examQuestion) {
        return toQuestionResponse(examQuestion, true);
    }

    private ExamQuestionResponse toQuestionResponse(ExamQuestion examQuestion, boolean includeCorrectness) {
        return ExamQuestionResponse.builder()
                .examQuestionId(examQuestion.getId())
                .questionId(examQuestion.getQuestion().getId())
                .orderIndex(examQuestion.getOrderIndex())
                .score(examQuestion.getScore())
                .subjectId(examQuestion.getQuestion().getSubject().getId())
                .topicId(examQuestion.getQuestion().getTopic() == null ? null : examQuestion.getQuestion().getTopic().getId())
                .content(examQuestion.getQuestion().getContent())
                .type(examQuestion.getQuestion().getType() == null ? null : examQuestion.getQuestion().getType().name())
                .difficulty(examQuestion.getQuestion().getDifficulty() == null ? null : examQuestion.getQuestion().getDifficulty().name())
                .answers(examQuestion.getQuestion().getAnswers().stream()
                        .map(answer -> ExamQuestionResponse.AnswerOptionResponse.builder()
                                .id(answer.getId())
                                .content(answer.getContent())
                                // Chỉ trả cờ đúng/sai và giải thích cho giáo viên/quản trị.
                                .correct(includeCorrectness ? answer.getIsCorrect() : null)
                                .explanation(includeCorrectness ? answer.getExplanation() : null)
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
