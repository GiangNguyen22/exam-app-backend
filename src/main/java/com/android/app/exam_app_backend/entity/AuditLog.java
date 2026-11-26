package com.android.app.exam_app_backend.entity;

import com.android.app.exam_app_backend.entity.enums.AuditResult;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;

@Entity
@Table(name = "audit_logs")
@Immutable
@Getter
@Setter
@NoArgsConstructor
public class AuditLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", updatable = false)
    private User user;

    @Column(nullable = false, updatable = false)
    private String action;

    @Column(name = "resource_type", updatable = false)
    private String resourceType;

    @Column(name = "resource_id", updatable = false)
    private Long resourceId;

    @Enumerated(EnumType.STRING)
    @Column(updatable = false)
    private AuditResult result;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String reason;

    @Column(columnDefinition = "JSON", updatable = false)
    private String meta;

    @PreUpdate
    private void preventUpdate() {
        throw new UnsupportedOperationException("AuditLog is immutable (INSERT ONLY) and cannot be updated.");
    }

    @PreRemove
    private void preventDelete() {
        throw new UnsupportedOperationException("AuditLog is immutable (INSERT ONLY) and cannot be deleted.");
    }
}
