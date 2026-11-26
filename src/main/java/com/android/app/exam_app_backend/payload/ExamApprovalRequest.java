package com.android.app.exam_app_backend.payload;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ExamApprovalRequest {

    private String comment;
    private String comments;

    public String resolveComment() {
        if (comment != null && !comment.trim().isEmpty()) {
            return comment.trim();
        }
        if (comments != null && !comments.trim().isEmpty()) {
            return comments.trim();
        }
        return null;
    }
}
