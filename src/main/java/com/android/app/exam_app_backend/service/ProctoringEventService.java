package com.android.app.exam_app_backend.service;

import com.android.app.exam_app_backend.entity.Exam;
import com.android.app.exam_app_backend.entity.ProctoringEvent;
import com.android.app.exam_app_backend.entity.User;
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

    private static final List<String> ALERT_EVENT_TYPES = List.of("FOCUS_LOST", "CONNECTION_LOST", "SCREENSHOT");
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

        ProctoringEvent event = new ProctoringEvent();
        event.setExam(exam);
        event.setStudent(student);
        event.setEventType(request.getEventType());
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
        switch (eventType) {
            case "FOCUS_LOST": return "Rời khỏi ứng dụng";
            case "CONNECTION_LOST": return "Mất kết nối";
            case "SCREENSHOT": return "Chụp màn hình";
            default: return eventType;
        }
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
