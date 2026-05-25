package com.android.app.exam_app_backend.entity;

import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_responses")
@Getter
@Setter
@NoArgsConstructor
public class StudentResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id", nullable = false)
    private ExamResult result;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "selected_answer_ids")
    private String selectedAnswerIds;

    @Column(name = "fill_content", columnDefinition = "TEXT")
    private String fillContent;

    @Column(name = "last_saved_at")
    private LocalDateTime lastSavedAt;

}
