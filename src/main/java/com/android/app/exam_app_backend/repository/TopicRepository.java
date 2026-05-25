package com.android.app.exam_app_backend.repository;

import com.android.app.exam_app_backend.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicRepository extends JpaRepository<Topic, Long> {
}
