package com.android.app.exam_app_backend.repository;

import com.android.app.exam_app_backend.entity.ProctoringEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ProctoringEventRepository extends JpaRepository<ProctoringEvent, Long> {

    List<ProctoringEvent> findByExamIdOrderByCreatedAtDesc(Long examId);

    List<ProctoringEvent> findByExamIdAndStudentIdOrderByCreatedAtDesc(Long examId, Long studentId);

    @Query(value = "SELECT pe.* FROM proctoring_events pe " +
            "WHERE pe.exam_id = :examId " +
            "AND pe.created_at = ( " +
            "  SELECT MAX(pe2.created_at) FROM proctoring_events pe2 " +
            "  WHERE pe2.exam_id = :examId AND pe2.student_id = pe.student_id " +
            ") ORDER BY pe.created_at DESC", nativeQuery = true)
    List<ProctoringEvent> findLatestPerStudentByExamId(@Param("examId") Long examId);

    long countByExamIdAndEventTypeInAndCreatedAtAfter(
        Long examId, List<String> eventTypes, LocalDateTime after);
}
