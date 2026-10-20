package com.android.app.exam_app_backend.repository;

import com.android.app.exam_app_backend.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findDistinctByGroups_IdIn(Collection<Long> groupIds);
}
