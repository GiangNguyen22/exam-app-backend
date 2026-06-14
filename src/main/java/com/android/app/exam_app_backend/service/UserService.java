package com.android.app.exam_app_backend.service;

import com.android.app.exam_app_backend.entity.User;
import com.android.app.exam_app_backend.entity.UserRole;
import com.android.app.exam_app_backend.exception.ResourceNotFoundException;
import com.android.app.exam_app_backend.payload.UserProfileResponse;
import com.android.app.exam_app_backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(String username) {
        User user = userRepository.findByUsernameWithRolesAndPermissions(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        List<String> roles = user.getUserRoles().stream()
                .map(UserRole::getRole)
                .map(role -> role.getName().startsWith("ROLE_") ? role.getName().substring("ROLE_".length()) : role.getName())
                .collect(Collectors.toList());

        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .studentId(user.getStudentId())
                .employeeCode(user.getEmployeeCode())
                .status(user.getStatus() == null ? null : user.getStatus().name())
                .roles(roles)
                .build();
    }
}
