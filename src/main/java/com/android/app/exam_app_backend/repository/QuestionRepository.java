package com.android.app.exam_app_backend.repository;

import com.android.app.exam_app_backend.entity.Question;
import com.android.app.exam_app_backend.entity.enums.Difficulty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findBySubjectIdAndDifficulty(Long subjectId, Difficulty difficulty);
    List<Question> findBySubjectIdAndTopicIdAndDifficulty(Long subjectId, Long topicId, Difficulty difficulty);
}
