package com.android.app.exam_app_backend.entity;

import com.android.app.exam_app_backend.entity.enums.ExamStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

@Entity
@Table(name = "exam_versions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_exam_version_no", columnNames = {"exam_id", "version_no"})
        },
        indexes = {
                @Index(name = "idx_exam_versions_exam_id", columnList = "exam_id"),
                @Index(name = "idx_exam_versions_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamVersion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ExamStatus status = ExamStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "integrity_tag", length = 128)
    private String integrityTag;

    @Version
    @Column(name = "entity_version")
    private Long entityVersion;
}
