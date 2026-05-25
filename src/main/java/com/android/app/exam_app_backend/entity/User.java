package com.android.app.exam_app_backend.entity;

import com.android.app.exam_app_backend.entity.enums.UserStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "userRoles")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(unique = true)
    private String email;

    private String phone;

    @Column(name = "student_id", unique = true)
    private String studentId;

    @Column(name = "employee_code", unique = true)
    private String employeeCode;

    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.ACTIVE;

    // One user can have many user_role assignments
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserRole> userRoles = new HashSet<>();

    // Questions created by this user
    @OneToMany(mappedBy = "createdBy")
    private Set<Question> createdQuestions = new HashSet<>();

    // Exams created by this user
    @OneToMany(mappedBy = "createdBy")
    private Set<Exam> createdExams = new HashSet<>();
}
