package com.android.app.exam_app_backend.service;

import com.android.app.exam_app_backend.entity.Answer;
import com.android.app.exam_app_backend.entity.Exam;
import com.android.app.exam_app_backend.entity.ExamQuestion;
import com.android.app.exam_app_backend.entity.ExamResult;
import com.android.app.exam_app_backend.entity.Question;
import com.android.app.exam_app_backend.entity.Subject;
import com.android.app.exam_app_backend.entity.Topic;
import com.android.app.exam_app_backend.entity.User;
import com.android.app.exam_app_backend.entity.enums.AuditResult;
import com.android.app.exam_app_backend.entity.enums.ExamResultStatus;
import com.android.app.exam_app_backend.exception.ResourceNotFoundException;
import com.android.app.exam_app_backend.payload.ExamCreateRequest;
import com.android.app.exam_app_backend.payload.ExamGenerateRequest;
import com.android.app.exam_app_backend.payload.ExamQuestionCreateRequest;
import com.android.app.exam_app_backend.payload.ExamQuestionResponse;
import com.android.app.exam_app_backend.payload.ExamResponse;
import com.android.app.exam_app_backend.payload.ExamSubmitRequest;
import com.android.app.exam_app_backend.repository.AnswerRepository;
import com.android.app.exam_app_backend.repository.ExamQuestionRepository;
import com.android.app.exam_app_backend.repository.ExamRepository;
import com.android.app.exam_app_backend.repository.ExamResultRepository;
import com.android.app.exam_app_backend.repository.QuestionRepository;
import com.android.app.exam_app_backend.repository.SubjectRepository;
import com.android.app.exam_app_backend.repository.TopicRepository;
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
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamResultRepository examResultRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public ExamService(ExamRepository examRepository,
                       ExamQuestionRepository examQuestionRepository,
                       ExamResultRepository examResultRepository,
                       QuestionRepository questionRepository,
                       AnswerRepository answerRepository,
                       SubjectRepository subjectRepository,
                       TopicRepository topicRepository,
                       UserRepository userRepository,
                       AuditLogService auditLogService) {
        this.examRepository = examRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.examResultRepository = examResultRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<ExamResponse> getExams() {
        return examRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExamQuestionResponse> getExamQuestions(Long examId) {
        if (!examRepository.existsById(examId)) {
            throw new ResourceNotFoundException("Exam not found: " + examId);
        }

        return examQuestionRepository.findByExamIdOrderByOrderIndexAsc(examId).stream()
                .map(this::toQuestionResponse)
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
                .build();
    }

    private ExamQuestionResponse toQuestionResponse(ExamQuestion examQuestion) {
        return ExamQuestionResponse.builder()
                .examQuestionId(examQuestion.getId())
                .questionId(examQuestion.getQuestion().getId())
                .orderIndex(examQuestion.getOrderIndex())
                .score(examQuestion.getScore())
                .content(examQuestion.getQuestion().getContent())
                .type(examQuestion.getQuestion().getType() == null ? null : examQuestion.getQuestion().getType().name())
                .difficulty(examQuestion.getQuestion().getDifficulty() == null ? null : examQuestion.getQuestion().getDifficulty().name())
                .answers(examQuestion.getQuestion().getAnswers().stream()
                        .map(answer -> ExamQuestionResponse.AnswerOptionResponse.builder()
                                .id(answer.getId())
                                .content(answer.getContent())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
