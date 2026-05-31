package com.android.app.exam_app_backend.controller;

import com.android.app.exam_app_backend.common.dto.ApiResponse;
import com.android.app.exam_app_backend.entity.Subject;
import com.android.app.exam_app_backend.entity.Topic;
import com.android.app.exam_app_backend.payload.SubjectResponse;
import com.android.app.exam_app_backend.payload.TopicResponse;
import com.android.app.exam_app_backend.repository.SubjectRepository;
import com.android.app.exam_app_backend.repository.TopicRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;

    public CatalogController(SubjectRepository subjectRepository,
                             TopicRepository topicRepository) {
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
    }

    @GetMapping("/subjects")
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> getSubjects() {
        List<SubjectResponse> subjects = subjectRepository.findAll(Sort.by("name").ascending())
                .stream()
                .map(this::toSubjectResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.<List<SubjectResponse>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Subjects retrieved")
                .data(subjects)
                .build());
    }

    @GetMapping("/subjects/{subjectId}/topics")
    public ResponseEntity<ApiResponse<List<TopicResponse>>> getTopicsBySubject(@PathVariable Long subjectId) {
        List<TopicResponse> topics = topicRepository.findBySubjectIdOrderByNameAsc(subjectId)
                .stream()
                .map(this::toTopicResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.<List<TopicResponse>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Topics retrieved")
                .data(topics)
                .build());
    }

    private SubjectResponse toSubjectResponse(Subject subject) {
        return SubjectResponse.builder()
                .id(subject.getId())
                .name(subject.getName())
                .description(subject.getDescription())
                .build();
    }

    private TopicResponse toTopicResponse(Topic topic) {
        return TopicResponse.builder()
                .id(topic.getId())
                .subjectId(topic.getSubject().getId())
                .name(topic.getName())
                .description(topic.getDescription())
                .build();
    }
}
