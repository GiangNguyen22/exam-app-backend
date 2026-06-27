package com.android.app.exam_app_backend.repository;

import com.android.app.exam_app_backend.entity.StudentGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {
}
