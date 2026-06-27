package com.android.app.exam_app_backend.controller;

import com.android.app.exam_app_backend.common.dto.ApiResponse;
import com.android.app.exam_app_backend.entity.Subject;
import com.android.app.exam_app_backend.entity.Topic;
import com.android.app.exam_app_backend.payload.SubjectResponse;
import com.android.app.exam_app_backend.payload.TopicResponse;
import com.android.app.exam_app_backend.repository.SubjectRepository;
import com.android.app.exam_app_backend.repository.TopicRepository;
import com.android.app.exam_app_backend.payload.SubjectCreateRequest;
import com.android.app.exam_app_backend.payload.TopicCreateRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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

    @PostMapping("/subjects")
    public ResponseEntity<ApiResponse<SubjectResponse>> createSubject(@RequestBody SubjectCreateRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject name is required.");
        }
        if (subjectRepository.findByNameIgnoreCase(request.getName().trim()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Subject already exists.");
        }
        Subject subject = new Subject();
        subject.setName(request.getName().trim());
        subject.setDescription(request.getDescription());
        Subject saved = subjectRepository.save(subject);
        return ResponseEntity.ok(ApiResponse.<SubjectResponse>builder()
                .success(true)
                .code(HttpStatus.CREATED.value())
                .message("Subject created")
                .data(toSubjectResponse(saved))
                .build());
    }

    @PostMapping("/subjects/{subjectId}/topics")
    public ResponseEntity<ApiResponse<TopicResponse>> createTopic(@PathVariable Long subjectId,
                                                                   @RequestBody TopicCreateRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic name is required.");
        }
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found."));
        if (topicRepository.findBySubjectIdAndNameIgnoreCase(subjectId, request.getName().trim()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Topic already exists in this subject.");
        }
        Topic topic = new Topic();
        topic.setSubject(subject);
        topic.setName(request.getName().trim());
        topic.setDescription(request.getDescription());
        Topic saved = topicRepository.save(topic);
        return ResponseEntity.ok(ApiResponse.<TopicResponse>builder()
                .success(true)
                .code(HttpStatus.CREATED.value())
                .message("Topic created")
                .data(toTopicResponse(saved))
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
