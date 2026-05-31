package com.android.app.exam_app_backend.repository;

import com.android.app.exam_app_backend.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
}

