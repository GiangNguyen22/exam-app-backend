package com.android.app.exam_app_backend.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiExplainRequest {

    @NotBlank
    private String question;

    private String correctAnswer;

    private String studentAnswer;

    private boolean correct;

    private String subject;
}
