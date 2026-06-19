package com.android.app.exam_app_backend.repository;

import com.android.app.exam_app_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    @Query("select distinct u from User u " +
            "left join fetch u.userRoles ur " +
            "left join fetch ur.role r " +
            "left join fetch r.permissions " +
            "where u.username = :username")
    Optional<User> findByUsernameWithRolesAndPermissions(@Param("username") String username);

    @Query("select distinct u from User u " +
            "left join fetch u.userRoles ur " +
            "left join fetch ur.role " +
            "order by u.id asc")
    List<User> findAllWithRoles();

    @Query("select distinct u from User u " +
            "left join fetch u.userRoles ur " +
            "left join fetch ur.role " +
            "where u.id = :id")
    Optional<User> findByIdWithRoles(@Param("id") Long id);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByStudentId(String studentId);

    boolean existsByEmployeeCode(String employeeCode);
}

