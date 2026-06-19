package com.android.app.exam_app_backend.config;

import com.android.app.exam_app_backend.entity.Permission;
import com.android.app.exam_app_backend.entity.Role;
import com.android.app.exam_app_backend.entity.Subject;
import com.android.app.exam_app_backend.entity.Topic;
import com.android.app.exam_app_backend.entity.User;
import com.android.app.exam_app_backend.entity.UserRole;
import com.android.app.exam_app_backend.repository.PermissionRepository;
import com.android.app.exam_app_backend.repository.RoleRepository;
import com.android.app.exam_app_backend.repository.SubjectRepository;
import com.android.app.exam_app_backend.repository.TopicRepository;
import com.android.app.exam_app_backend.repository.UserRepository;
import com.android.app.exam_app_backend.repository.UserRoleRepository;
import com.android.app.exam_app_backend.security.PermissionConstants;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner createDefaultUser(UserRepository userRepository,
                                               RoleRepository roleRepository,
                                               PermissionRepository permissionRepository,
                                               UserRoleRepository userRoleRepository,
                                               SubjectRepository subjectRepository,
                                               TopicRepository topicRepository,
                                               PasswordEncoder passwordEncoder) {
        return args -> {
            Set<String> defaultPermissions = new LinkedHashSet<>(Arrays.asList(
                    PermissionConstants.QUESTION_CREATE,
                    PermissionConstants.QUESTION_VIEW,
                    PermissionConstants.QUESTION_UPDATE,
                    PermissionConstants.QUESTION_DELETE,
                    PermissionConstants.QUESTION_IMPORT,
                    PermissionConstants.USER_VIEW,
                    PermissionConstants.USER_CREATE,
                    PermissionConstants.USER_UPDATE,
                    PermissionConstants.USER_LOCK,
                    PermissionConstants.AUDIT_VIEW,
                    PermissionConstants.EXAM_CREATE,
                    PermissionConstants.EXAM_GENERATE,
                    PermissionConstants.EXAM_SUBMIT,
                    PermissionConstants.EXAM_VIEW_RESULTS,
                    PermissionConstants.EXAM_VIEW_OWN_RESULTS
            ));

            Map<String, Permission> permissionMap = defaultPermissions.stream()
                    .collect(Collectors.toMap(
                            p -> p,
                            permissionName -> permissionRepository.findByName(permissionName)
                                    .orElseGet(() -> {
                                        Permission p = new Permission();
                                        p.setName(permissionName);
                                        p.setDescription("Bootstrapped permission: " + permissionName);
                                        return permissionRepository.save(p);
                                    })
                    ));

            Role adminRole = upsertRole(roleRepository, "ADMIN", "System administrator");
            Role teacherRole = upsertRole(roleRepository, "TEACHER", "Teacher role");
            Role studentRole = upsertRole(roleRepository, "STUDENT", "Student role");

            adminRole.setPermissions(new HashSet<>(permissionMap.values()));
            teacherRole.setPermissions(new HashSet<>(Arrays.asList(
                    permissionMap.get(PermissionConstants.QUESTION_CREATE),
                    permissionMap.get(PermissionConstants.QUESTION_VIEW),
                    permissionMap.get(PermissionConstants.QUESTION_UPDATE),
                    permissionMap.get(PermissionConstants.QUESTION_DELETE),
                    permissionMap.get(PermissionConstants.QUESTION_IMPORT),
                    permissionMap.get(PermissionConstants.EXAM_CREATE),
                    permissionMap.get(PermissionConstants.EXAM_GENERATE),
                    permissionMap.get(PermissionConstants.EXAM_VIEW_RESULTS)
            )));
            studentRole.setPermissions(new HashSet<>(Arrays.asList(
                    permissionMap.get(PermissionConstants.EXAM_SUBMIT),
                    permissionMap.get(PermissionConstants.EXAM_VIEW_OWN_RESULTS)
            )));

            roleRepository.save(adminRole);
            roleRepository.save(teacherRole);
            roleRepository.save(studentRole);

            User admin = upsertUser(userRepository, passwordEncoder, "admin", "admin123", "Administrator", "admin@example.com");
            User teacher = upsertUser(userRepository, passwordEncoder, "teacher1", "teacher123", "Teacher One", "teacher1@example.com");
            User student = upsertUser(userRepository, passwordEncoder, "student1", "student123", "Student One", "student1@example.com");

            assignRoleIfMissing(userRoleRepository, admin, adminRole);
            assignRoleIfMissing(userRoleRepository, teacher, teacherRole);
            assignRoleIfMissing(userRoleRepository, student, studentRole);

            if (subjectRepository.count() == 0) {
                Subject subject = new Subject();
                subject.setName("General Knowledge");
                subject.setDescription("Default subject for bootstrap");
                Subject savedSubject = subjectRepository.save(subject);

                Topic topic = new Topic();
                topic.setName("Basics");
                topic.setDescription("Default topic for bootstrap");
                topic.setSubject(savedSubject);
                topicRepository.save(topic);
            }
        };
    }

    private Role upsertRole(RoleRepository roleRepository, String name, String description) {
        return roleRepository.findByName(name)
                .map(role -> {
                    role.setDescription(description);
                    return role;
                })
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(name);
                    role.setDescription(description);
                    return roleRepository.save(role);
                });
    }

    private User upsertUser(UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            String username,
                            String rawPassword,
                            String fullName,
                            String email) {
        return userRepository.findByUsername(username).orElseGet(() -> {
            User user = new User();
            user.setUsername(username);
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            user.setFullName(fullName);
            user.setEmail(email);
            return userRepository.save(user);
        });
    }

    private void assignRoleIfMissing(UserRoleRepository userRoleRepository, User user, Role role) {
        if (userRoleRepository.findByUserAndRole(user, role).isEmpty()) {
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            userRoleRepository.save(userRole);
        }
    }
}

