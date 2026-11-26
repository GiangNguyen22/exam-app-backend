package com.android.app.exam_app_backend.repository;

import com.android.app.exam_app_backend.entity.Exam;
import com.android.app.exam_app_backend.entity.enums.ExamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findDistinctByGroups_IdIn(Collection<Long> groupIds);
    List<Exam> findByCreatedById(Long createdById);

    @Query("SELECT DISTINCT e FROM Exam e JOIN FETCH e.currentVersion v WHERE v.status IN :statuses ORDER BY e.createdAt DESC")
    List<Exam> findByCurrentVersionStatusIn(@Param("statuses") Collection<ExamStatus> statuses);
}
