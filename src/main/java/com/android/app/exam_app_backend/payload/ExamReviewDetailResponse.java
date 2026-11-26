package com.android.app.exam_app_backend.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamReviewDetailResponse {
    private ExamResponse exam;
    private List<ExamQuestionResponse> questions;
    private List<ExamVersionResponse> versions;
    private List<ExamApprovalResponse> approvals;
    private Boolean integritySigned;
}
