package com.android.app.exam_app_backend.payload;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProctoringSummaryResponse {
    private List<StudentProctoringStatus> students;

    @Getter
    @Builder
    public static class StudentProctoringStatus {
        private Long studentId;
        private String studentName;
        private String username;
        private String latestEventType;
        private String latestDetails;
        private String latestEventAt;
        private Boolean hasAlert;
        private String alertLabel;
    }
}
