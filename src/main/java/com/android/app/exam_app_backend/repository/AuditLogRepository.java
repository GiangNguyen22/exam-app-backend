package com.android.app.exam_app_backend.repository;

import com.android.app.exam_app_backend.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
