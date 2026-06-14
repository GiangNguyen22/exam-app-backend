package com.android.app.exam_app_backend.repository;

import com.android.app.exam_app_backend.entity.ExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {
    List<ExamQuestion> findByExamIdOrderByOrderIndexAsc(Long examId);
    Optional<ExamQuestion> findByExamIdAndQuestionId(Long examId, Long questionId);
}
