package com.android.app.exam_app_backend.repository;

import com.android.app.exam_app_backend.entity.Role;
import com.android.app.exam_app_backend.entity.User;
import com.android.app.exam_app_backend.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    Optional<UserRole> findByUserAndRole(User user, Role role);

    void deleteByUser(User user);
}
