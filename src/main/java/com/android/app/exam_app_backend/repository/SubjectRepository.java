package com.android.app.exam_app_backend.repository;

import com.android.app.exam_app_backend.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
}
