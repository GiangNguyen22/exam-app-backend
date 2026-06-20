package com.android.app.exam_app_backend.payload;

import com.android.app.exam_app_backend.entity.enums.ExamResultStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ExamReportResponse {
    private Long examId;
    private String examCode;
    private String examTitle;
    private Integer totalResults;
    private Integer submittedCount;
    private Integer doingCount;
    private BigDecimal averageScore;
    private BigDecimal highestScore;
    private BigDecimal lowestScore;
    private List<StudentResult> results;

    @Getter
    @Builder
    public static class StudentResult {
        private Long resultId;
        private Long studentId;
        private String studentCode;
        private String studentName;
        private String username;
        private BigDecimal score;
        private ExamResultStatus status;
        private LocalDateTime startedAt;
        private LocalDateTime submittedAt;
    }
}
