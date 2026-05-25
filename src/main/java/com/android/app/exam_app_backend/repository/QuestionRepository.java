package com.android.app.exam_app_backend.repository;

import com.android.app.exam_app_backend.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
}
