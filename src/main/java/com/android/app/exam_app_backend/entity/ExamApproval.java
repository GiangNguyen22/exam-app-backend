package com.android.app.exam_app_backend.entity;

import com.android.app.exam_app_backend.entity.enums.ApprovalDecision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Immutable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_approvals",
        indexes = {
                @Index(name = "idx_exam_approvals_version_id", columnList = "exam_version_id"),
                @Index(name = "idx_exam_approvals_approver_id", columnList = "approver_id"),
                @Index(name = "idx_exam_approvals_decision", columnList = "decision")
        }
)
@Check(constraints = "approver_id <> creator_id")
@Immutable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamApproval extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_version_id", nullable = false, updatable = false)
    private ExamVersion examVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id", nullable = false, updatable = false)
    private User approver;

    @Column(name = "creator_id", nullable = false, updatable = false)
    private Long creatorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private ApprovalDecision decision;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String comment;

    @Column(name = "decided_at", nullable = false, updatable = false)
    private LocalDateTime decidedAt;

    @PreUpdate
    private void preventUpdate() {
        throw new UnsupportedOperationException("ExamApproval is append-only and cannot be updated.");
    }

    @PreRemove
    private void preventDelete() {
        throw new UnsupportedOperationException("ExamApproval is append-only and cannot be deleted.");
    }
}
