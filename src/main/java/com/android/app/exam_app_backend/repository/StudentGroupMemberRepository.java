package com.android.app.exam_app_backend.repository;

import com.android.app.exam_app_backend.entity.StudentGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentGroupMemberRepository extends JpaRepository<StudentGroupMember, Long> {
    List<StudentGroupMember> findByGroupId(Long groupId);
    Optional<StudentGroupMember> findByGroupIdAndUserId(Long groupId, Long userId);
    List<StudentGroupMember> findByUserId(Long userId);
}
