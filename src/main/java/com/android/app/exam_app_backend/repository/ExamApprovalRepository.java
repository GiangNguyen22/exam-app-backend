package com.android.app.exam_app_backend.repository;

import com.android.app.exam_app_backend.entity.ExamApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamApprovalRepository extends JpaRepository<ExamApproval, Long> {

    @Query("SELECT a FROM ExamApproval a JOIN FETCH a.approver JOIN FETCH a.examVersion v WHERE v.exam.id = :examId ORDER BY a.decidedAt DESC, a.id DESC")
    List<ExamApproval> findByExamIdWithDetails(@Param("examId") Long examId);

    List<ExamApproval> findByExamVersionIdOrderByDecidedAtDesc(Long examVersionId);
}
