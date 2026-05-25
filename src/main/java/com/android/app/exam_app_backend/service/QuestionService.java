package com.android.app.exam_app_backend.service;

import com.android.app.exam_app_backend.entity.Question;
import com.android.app.exam_app_backend.entity.Subject;
import com.android.app.exam_app_backend.entity.Topic;
import com.android.app.exam_app_backend.entity.User;
import com.android.app.exam_app_backend.entity.enums.AuditResult;
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

        Question saved = questionRepository.save(question);
        auditLogService.log(currentUser, PermissionConstants.QUESTION_CREATE, "question", saved.getId(), AuditResult.allow, "Question created");
        return QuestionResponse.builder()
                .id(saved.getId())
                .content(saved.getContent())
                .build();
    }
}
