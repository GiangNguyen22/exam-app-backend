package com.android.app.exam_app_backend.service;

import com.android.app.exam_app_backend.entity.Answer;
import com.android.app.exam_app_backend.entity.Question;
import com.android.app.exam_app_backend.entity.Subject;
import com.android.app.exam_app_backend.entity.Topic;
import com.android.app.exam_app_backend.entity.User;
import com.android.app.exam_app_backend.entity.enums.AuditResult;
import com.android.app.exam_app_backend.entity.enums.QuestionType;
import com.android.app.exam_app_backend.exception.ResourceNotFoundException;
import com.android.app.exam_app_backend.payload.QuestionCreateRequest;
import com.android.app.exam_app_backend.payload.QuestionResponse;
import com.android.app.exam_app_backend.repository.QuestionRepository;
import com.android.app.exam_app_backend.repository.SubjectRepository;
import com.android.app.exam_app_backend.repository.TopicRepository;
import com.android.app.exam_app_backend.repository.UserRepository;
import com.android.app.exam_app_backend.security.PermissionConstants;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public QuestionService(QuestionRepository questionRepository,
                           SubjectRepository subjectRepository,
                           TopicRepository topicRepository,
                           UserRepository userRepository,
                           AuditLogService auditLogService) {
        this.questionRepository = questionRepository;
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, T(com.android.app.exam_app_backend.security.PermissionConstants).QUESTION_VIEW)")
    public List<QuestionResponse> getQuestions() {
        return questionRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, T(com.android.app.exam_app_backend.security.PermissionConstants).QUESTION_CREATE)")
    public QuestionResponse createQuestion(QuestionCreateRequest request, Authentication authentication) {
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + request.getSubjectId()));

        Topic topic = null;
        if (request.getTopicId() != null) {
            topic = topicRepository.findById(request.getTopicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + request.getTopicId()));
        }

        User currentUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));

        Question question = new Question();
        question.setSubject(subject);
        question.setTopic(topic);
        question.setContent(request.getContent());
        question.setType(request.getType());
        question.setDifficulty(request.getDifficulty());
        question.setCreatedBy(currentUser);
        applyAnswers(question, request);

        Question saved = questionRepository.save(question);
        auditLogService.log(currentUser, PermissionConstants.QUESTION_CREATE, "question", saved.getId(), AuditResult.allow, "Question created");
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, T(com.android.app.exam_app_backend.security.PermissionConstants).QUESTION_UPDATE)")
    public QuestionResponse updateQuestion(Long questionId, QuestionCreateRequest request, Authentication authentication) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + request.getSubjectId()));

        Topic topic = null;
        if (request.getTopicId() != null) {
            topic = topicRepository.findById(request.getTopicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + request.getTopicId()));
        }

        User currentUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));

        question.setSubject(subject);
        question.setTopic(topic);
        question.setContent(request.getContent());
        question.setType(request.getType());
        question.setDifficulty(request.getDifficulty());
        applyAnswers(question, request);

        Question saved = questionRepository.save(question);
        auditLogService.log(currentUser, PermissionConstants.QUESTION_UPDATE, "question", saved.getId(), AuditResult.allow, "Question updated");
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, T(com.android.app.exam_app_backend.security.PermissionConstants).QUESTION_DELETE)")
    public void deleteQuestion(Long questionId, Authentication authentication) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));
        User currentUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));

        questionRepository.delete(question);
        auditLogService.log(currentUser, PermissionConstants.QUESTION_DELETE, "question", questionId, AuditResult.allow, "Question deleted");
    }

    private void applyAnswers(Question question, QuestionCreateRequest request) {
        validateAnswers(request);
        question.getAnswers().clear();
        request.getAnswers().forEach(answerRequest -> {
            Answer answer = new Answer();
            answer.setQuestion(question);
            answer.setContent(answerRequest.getContent().trim());
            answer.setIsCorrect(Boolean.TRUE.equals(answerRequest.getCorrect()));
            answer.setExplanation(blankToNull(answerRequest.getExplanation()));
            question.getAnswers().add(answer);
        });
    }

    private void validateAnswers(QuestionCreateRequest request) {
        List<QuestionCreateRequest.AnswerRequest> answers = request.getAnswers().stream()
                .filter(answer -> answer.getContent() != null && !answer.getContent().trim().isBlank())
                .collect(Collectors.toList());
        if (answers.isEmpty()) {
            throw new IllegalArgumentException("At least one answer is required");
        }

        long correctCount = answers.stream()
                .filter(answer -> Boolean.TRUE.equals(answer.getCorrect()))
                .count();

        if (request.getType() == QuestionType.FILL_BLANK) {
            if (answers.size() != 1 || correctCount != 1) {
                throw new IllegalArgumentException("Fill blank questions require exactly one correct answer");
            }
            return;
        }

        if (request.getType() == QuestionType.TRUE_FALSE) {
            if (answers.size() != 2 || correctCount != 1) {
                throw new IllegalArgumentException("True/false questions require exactly two answers and one correct answer");
            }
            return;
        }

        if (request.getType() == QuestionType.SINGLE) {
            if (answers.size() < 2 || correctCount != 1) {
                throw new IllegalArgumentException("Single choice questions require at least two answers and exactly one correct answer");
            }
            return;
        }

        if (answers.size() < 2 || correctCount < 1) {
            throw new IllegalArgumentException("Multiple choice questions require at least two answers and at least one correct answer");
        }
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private QuestionResponse toResponse(Question question) {
        return QuestionResponse.builder()
                .id(question.getId())
                .subjectId(question.getSubject() == null ? null : question.getSubject().getId())
                .subjectName(question.getSubject() == null ? null : question.getSubject().getName())
                .topicId(question.getTopic() == null ? null : question.getTopic().getId())
                .topicName(question.getTopic() == null ? null : question.getTopic().getName())
                .content(question.getContent())
                .type(question.getType() == null ? null : question.getType().name())
                .difficulty(question.getDifficulty() == null ? null : question.getDifficulty().name())
                .answers(question.getAnswers().stream()
                        .map(answer -> QuestionResponse.AnswerOptionResponse.builder()
                                .id(answer.getId())
                                .content(answer.getContent())
                                .correct(answer.getIsCorrect())
                                .explanation(answer.getExplanation())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
