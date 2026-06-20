package com.android.app.exam_app_backend.repository;

import com.android.app.exam_app_backend.entity.Question;
import com.android.app.exam_app_backend.entity.enums.Difficulty;
import com.android.app.exam_app_backend.entity.enums.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findBySubjectIdAndDifficulty(Long subjectId, Difficulty difficulty);
    List<Question> findBySubjectIdAndTopicIdAndDifficulty(Long subjectId, Long topicId, Difficulty difficulty);

    @Query("select case when count(q) > 0 then true else false end from Question q " +
            "where q.subject.id = :subjectId and " +
            "((:topicId is null and q.topic is null) or q.topic.id = :topicId) and " +
            "lower(q.content) = lower(:content) and q.type = :type and q.difficulty = :difficulty")
    boolean existsDuplicateQuestion(
            @Param("subjectId") Long subjectId,
            @Param("topicId") Long topicId,
            @Param("content") String content,
            @Param("type") QuestionType type,
            @Param("difficulty") Difficulty difficulty
    );
}
