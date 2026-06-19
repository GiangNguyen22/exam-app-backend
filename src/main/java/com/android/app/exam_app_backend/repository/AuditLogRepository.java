package com.android.app.exam_app_backend.repository;

import com.android.app.exam_app_backend.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("select a from AuditLog a left join fetch a.user order by a.createdAt desc")
    List<AuditLog> findAllWithUserOrderByCreatedAtDesc();
}
