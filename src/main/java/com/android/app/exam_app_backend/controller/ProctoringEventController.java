package com.android.app.exam_app_backend.controller;

import com.android.app.exam_app_backend.common.dto.ApiResponse;
import com.android.app.exam_app_backend.entity.User;
import com.android.app.exam_app_backend.payload.ProctoringEventRequest;
import com.android.app.exam_app_backend.payload.ProctoringEventResponse;
import com.android.app.exam_app_backend.payload.ProctoringSummaryResponse;
import com.android.app.exam_app_backend.repository.UserRepository;
import com.android.app.exam_app_backend.service.ProctoringEventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exams/{examId}/proctoring-events")
public class ProctoringEventController {

    private final ProctoringEventService service;
    private final UserRepository userRepository;

    public ProctoringEventController(ProctoringEventService service, UserRepository userRepository) {
        this.service = service;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProctoringEventResponse>> createEvent(
            @PathVariable Long examId,
            @RequestBody ProctoringEventRequest request,
            Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found: " + authentication.getName()));
        return ResponseEntity.ok(ApiResponse.<ProctoringEventResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Event created")
                .data(service.create(examId, user.getId(), request))
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProctoringEventResponse>>> getEvents(
            @PathVariable Long examId) {
        return ResponseEntity.ok(ApiResponse.<List<ProctoringEventResponse>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Events retrieved")
                .data(service.getEvents(examId))
                .build());
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ProctoringSummaryResponse>> getSummary(
            @PathVariable Long examId) {
        return ResponseEntity.ok(ApiResponse.<ProctoringSummaryResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Summary retrieved")
                .data(service.getSummary(examId))
                .build());
    }
}
