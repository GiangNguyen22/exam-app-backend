package com.android.app.exam_app_backend.repository;

import com.android.app.exam_app_backend.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    List<Topic> findBySubjectIdOrderByNameAsc(Long subjectId);
    Optional<Topic> findBySubjectIdAndNameIgnoreCase(Long subjectId, String name);
}
