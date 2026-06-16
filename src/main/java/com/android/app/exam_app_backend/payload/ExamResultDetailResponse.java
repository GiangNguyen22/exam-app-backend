package com.android.app.exam_app_backend.payload;

import com.android.app.exam_app_backend.entity.enums.ExamResultStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Kết quả chi tiết của một lần làm bài: điểm tổng, số câu đúng/sai/bỏ trống,
 * và đáp án đúng kèm bài làm của học sinh cho từng câu. Chỉ trả về sau khi đã nộp.
 */
@Getter
@Builder
public class ExamResultDetailResponse {
    private Long resultId;
    private Long examId;
    private String examTitle;
    private BigDecimal score;
    private ExamResultStatus status;
    private LocalDateTime submittedAt;
    private Integer totalQuestions;
    private Integer correctCount;
    private Integer wrongCount;
    private Integer blankCount;
    private List<QuestionResult> questions;

    @Getter
    @Builder
    public static class QuestionResult {
        private Long questionId;
        private Integer orderIndex;
        private String content;
        private String type;
        private Boolean correct;
        private Boolean blank;
        private List<Long> selectedAnswerIds;
        private String fillContent;
        private List<AnswerResult> answers;
    }

    @Getter
    @Builder
    public static class AnswerResult {
        private Long id;
        private String content;
        private Boolean correct;
        private String explanation;
    }
}
