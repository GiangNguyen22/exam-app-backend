package com.android.app.exam_app_backend.repository;

import com.android.app.exam_app_backend.entity.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {
    Optional<ExamResult> findTopByExamIdOrderByIdDesc(Long examId);
    Optional<ExamResult> findByStudentIdAndExamId(Long studentId, Long examId);
    List<ExamResult> findByExamIdOrderByIdDesc(Long examId);
}
