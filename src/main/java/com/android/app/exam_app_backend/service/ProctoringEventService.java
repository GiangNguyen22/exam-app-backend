package com.android.app.exam_app_backend.service;

import com.android.app.exam_app_backend.entity.Exam;
import com.android.app.exam_app_backend.entity.ProctoringEvent;
import com.android.app.exam_app_backend.entity.User;
import com.android.app.exam_app_backend.entity.enums.SecurityEventType;
import com.android.app.exam_app_backend.payload.ProctoringEventRequest;
import com.android.app.exam_app_backend.payload.ProctoringEventResponse;
import com.android.app.exam_app_backend.payload.ProctoringSummaryResponse;
import com.android.app.exam_app_backend.repository.ExamRepository;
import com.android.app.exam_app_backend.repository.ProctoringEventRepository;
import com.android.app.exam_app_backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProctoringEventService {

    private final ProctoringEventRepository repository;
    private final ExamRepository examRepository;
    private final UserRepository userRepository;

    private static final List<String> ALERT_EVENT_TYPES = List.of(
            SecurityEventType.APP_BACKGROUND.name(),
            "FOCUS_LOST",
            SecurityEventType.SCREENSHOT_ATTEMPT.name(),
            "SCREENSHOT",
            SecurityEventType.SPLIT_SCREEN_DETECTED.name(),
            "SPLIT_SCREEN",
            SecurityEventType.CONNECTION_LOST.name()
    );
    private static final long ALERT_WINDOW_SECONDS = 120;

    public ProctoringEventService(ProctoringEventRepository repository,
                                  ExamRepository examRepository,
                                  UserRepository userRepository) {
        this.repository = repository;
        this.examRepository = examRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProctoringEventResponse create(Long examId, Long studentId, ProctoringEventRequest request) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found: " + examId));
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));

        SecurityEventType normalizedType = SecurityEventType.fromString(request.getEventType());
        String eventType = normalizedType != null ? normalizedType.name() : request.getEventType();

        ProctoringEvent event = new ProctoringEvent();
        event.setExam(exam);
        event.setStudent(student);
        event.setEventType(eventType);
        event.setDetails(request.getDetails());
        event = repository.save(event);

        return toResponse(event);
    }

    @Transactional(readOnly = true)
    public List<ProctoringEventResponse> getEvents(Long examId) {
        return repository.findByExamIdOrderByCreatedAtDesc(examId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProctoringEventResponse> getStudentEvents(Long examId, Long studentId) {
        return repository.findByExamIdAndStudentIdOrderByCreatedAtDesc(examId, studentId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProctoringSummaryResponse getSummary(Long examId) {
        List<ProctoringEvent> latestEvents = repository.findLatestPerStudentByExamId(examId);
        LocalDateTime alertThreshold = LocalDateTime.now().minusSeconds(ALERT_WINDOW_SECONDS);

        List<ProctoringSummaryResponse.StudentProctoringStatus> statuses = latestEvents.stream()
                .map(event -> {
                    User student = event.getStudent();
                    boolean hasAlert = ALERT_EVENT_TYPES.contains(event.getEventType())
                            && event.getCreatedAt().isAfter(alertThreshold);
                    String alertLabel = hasAlert ? getAlertLabel(event.getEventType()) : null;
                    return ProctoringSummaryResponse.StudentProctoringStatus.builder()
                            .studentId(student.getId())
                            .studentName(student.getFullName())
                            .username(student.getUsername())
                            .latestEventType(event.getEventType())
                            .latestDetails(event.getDetails())
                            .latestEventAt(event.getCreatedAt().toString())
                            .hasAlert(hasAlert)
                            .alertLabel(alertLabel)
                            .build();
                })
                .sorted(Comparator.comparing(ProctoringSummaryResponse.StudentProctoringStatus::getHasAlert).reversed())
                .collect(Collectors.toList());

        return ProctoringSummaryResponse.builder().students(statuses).build();
    }

    private String getAlertLabel(String eventType) {
        SecurityEventType type = SecurityEventType.fromString(eventType);
        if (type != null) {
            switch (type) {
                case APP_BACKGROUND: return "Rời khỏi ứng dụng";
                case APP_FOREGROUND: return "Quay lại ứng dụng";
                case SCREENSHOT_ATTEMPT: return "Chụp màn hình";
                case SPLIT_SCREEN_DETECTED: return "Chia đôi màn hình";
                case CONNECTION_LOST: return "Mất kết nối";
                case CONNECTION_RESTORED: return "Khôi phục kết nối";
            }
        }
        return eventType;
    }

    private ProctoringEventResponse toResponse(ProctoringEvent event) {
        User student = event.getStudent();
        return ProctoringEventResponse.builder()
                .id(event.getId())
                .examId(event.getExam().getId())
                .studentId(student.getId())
                .studentName(student.getFullName())
                .username(student.getUsername())
                .eventType(event.getEventType())
                .details(event.getDetails())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
