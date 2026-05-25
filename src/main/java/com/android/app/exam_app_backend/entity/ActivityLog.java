package com.android.app.exam_app_backend.entity;

import com.android.app.exam_app_backend.entity.enums.ActivityEventType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "activity_logs")
@Getter
@Setter
@NoArgsConstructor
public class ActivityLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id", nullable = false)
    private ExamResult result;

    @Enumerated(EnumType.STRING)
    private ActivityEventType eventType;

    @Column(columnDefinition = "TEXT")
    private String details;

}
