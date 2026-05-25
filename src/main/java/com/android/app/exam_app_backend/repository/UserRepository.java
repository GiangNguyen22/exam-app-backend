package com.android.app.exam_app_backend.repository;

import com.android.app.exam_app_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    @Query("select distinct u from User u " +
            "left join fetch u.userRoles ur " +
            "left join fetch ur.role r " +
            "left join fetch r.permissions " +
            "where u.username = :username")
    Optional<User> findByUsernameWithRolesAndPermissions(@Param("username") String username);
}

