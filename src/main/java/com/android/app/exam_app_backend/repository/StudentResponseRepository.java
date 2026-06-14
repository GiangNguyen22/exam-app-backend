package com.android.app.exam_app_backend.repository;

import com.android.app.exam_app_backend.entity.StudentResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentResponseRepository extends JpaRepository<StudentResponse, Long> {
    List<StudentResponse> findByResultId(Long resultId);
    void deleteByResultId(Long resultId);
}
