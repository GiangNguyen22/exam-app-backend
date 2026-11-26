package com.android.app.exam_app_backend.repository;

import com.android.app.exam_app_backend.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("select a from AuditLog a left join fetch a.user order by a.createdAt desc, a.id desc")
    List<AuditLog> findAllWithUserOrderByCreatedAtDesc();

    @Query("select a from AuditLog a left join fetch a.user where (:action is null or a.action = :action) and (:resourceType is null or a.resourceType = :resourceType) and (:resourceId is null or a.resourceId = :resourceId) order by a.createdAt desc, a.id desc")
    List<AuditLog> searchLogs(@Param("action") String action,
                              @Param("resourceType") String resourceType,
                              @Param("resourceId") Long resourceId);
}
