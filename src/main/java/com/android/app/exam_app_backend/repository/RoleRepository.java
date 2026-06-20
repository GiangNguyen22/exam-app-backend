package com.android.app.exam_app_backend.repository;

import com.android.app.exam_app_backend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);

    @Query("select distinct r from Role r left join fetch r.permissions order by r.id asc")
    List<Role> findAllWithPermissions();

    @Query("select distinct r from Role r left join fetch r.permissions where r.id = :id")
    Optional<Role> findByIdWithPermissions(@Param("id") Long id);
}
