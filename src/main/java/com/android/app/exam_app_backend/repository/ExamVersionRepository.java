package com.android.app.exam_app_backend.repository;

import com.android.app.exam_app_backend.entity.ExamVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamVersionRepository extends JpaRepository<ExamVersion, Long> {

    List<ExamVersion> findByExamIdOrderByVersionNoDesc(Long examId);

    Optional<ExamVersion> findByExamIdAndVersionNo(Long examId, Integer versionNo);
}
