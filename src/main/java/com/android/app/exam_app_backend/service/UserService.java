package com.android.app.exam_app_backend.service;

import com.android.app.exam_app_backend.entity.Role;
import com.android.app.exam_app_backend.entity.User;
import com.android.app.exam_app_backend.entity.UserRole;
import com.android.app.exam_app_backend.entity.enums.UserStatus;
import com.android.app.exam_app_backend.exception.ResourceNotFoundException;
import com.android.app.exam_app_backend.payload.UserCreateRequest;
import com.android.app.exam_app_backend.payload.UserProfileResponse;
import com.android.app.exam_app_backend.payload.UserUpdateRequest;
import com.android.app.exam_app_backend.repository.RoleRepository;
import com.android.app.exam_app_backend.repository.UserRepository;
import com.android.app.exam_app_backend.repository.UserRoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(String username) {
        User user = userRepository.findByUsernameWithRolesAndPermissions(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        return toProfileResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> getUsers() {
        return userRepository.findAllWithRoles().stream()
                .map(this::toProfileResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserProfileResponse createUser(UserCreateRequest request, String actorUsername) {
        String studentId = resolveStudentId(request);
        String employeeCode = resolveEmployeeCode(request);
        validateCreateRequest(request, studentId, employeeCode);

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setEmail(blankToNull(request.getEmail()));
        user.setPhone(blankToNull(request.getPhone()));
        user.setStudentId(studentId);
        user.setEmployeeCode(employeeCode);
        user.setStatus(request.getStatus() == null ? UserStatus.ACTIVE : request.getStatus());

        User savedUser = userRepository.save(user);
        replaceRoles(savedUser, request.getRoles(), actorUsername);
        return toProfileResponse(userRepository.findByIdWithRoles(savedUser.getId()).orElse(savedUser));
    }

    @Transactional
    public UserProfileResponse updateUser(Long userId, UserUpdateRequest request, String actorUsername) {
        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (request.getFullName() != null) {
            String fullName = request.getFullName().trim();
            if (fullName.isBlank()) {
                throw new IllegalArgumentException("Full name must not be blank");
            }
            user.setFullName(fullName);
        }
        updateUniqueFields(user, request);
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        User savedUser = userRepository.save(user);
        if (request.getRoles() != null) {
            replaceRoles(savedUser, request.getRoles(), actorUsername);
        }
        return toProfileResponse(userRepository.findByIdWithRoles(savedUser.getId()).orElse(savedUser));
    }

    @Transactional
    public UserProfileResponse updateUserStatus(Long userId, UserStatus status) {
        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setStatus(status);
        return toProfileResponse(userRepository.save(user));
    }

    @Transactional
    public UserProfileResponse updateUserRoles(Long userId, List<String> roles, String actorUsername) {
        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        replaceRoles(user, roles, actorUsername);
        return toProfileResponse(userRepository.findByIdWithRoles(userId).orElse(user));
    }

    private UserProfileResponse toProfileResponse(User user) {
        List<String> roles = user.getUserRoles().stream()
                .map(UserRole::getRole)
                .map(role -> normalizeRoleLabel(role.getName()))
                .sorted()
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

    private void validateCreateRequest(UserCreateRequest request, String studentId, String employeeCode) {
        String username = request.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        String email = blankToNull(request.getEmail());
        if (email != null && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (studentId != null && userRepository.existsByStudentId(studentId)) {
            throw new IllegalArgumentException("Student ID already exists");
        }
        if (employeeCode != null && userRepository.existsByEmployeeCode(employeeCode)) {
            throw new IllegalArgumentException("Employee code already exists");
        }
    }

    private String resolveStudentId(UserCreateRequest request) {
        String studentId = blankToNull(request.getStudentId());
        if (studentId != null || !hasRole(request.getRoles(), "STUDENT")) {
            return studentId;
        }
        return generateUniqueStudentId();
    }

    private String resolveEmployeeCode(UserCreateRequest request) {
        String employeeCode = blankToNull(request.getEmployeeCode());
        if (employeeCode != null || (!hasRole(request.getRoles(), "TEACHER") && !hasRole(request.getRoles(), "ADMIN"))) {
            return employeeCode;
        }
        return generateUniqueEmployeeCode();
    }

    private String generateUniqueStudentId() {
        return generateUniqueCode("SV", code -> userRepository.existsByStudentId(code));
    }

    private String generateUniqueEmployeeCode() {
        return generateUniqueCode("NV", code -> userRepository.existsByEmployeeCode(code));
    }

    private String generateUniqueCode(String prefix, java.util.function.Predicate<String> exists) {
        int year = Year.now().getValue();
        for (int index = 1; index <= 9999; index++) {
            String code = String.format(Locale.ROOT, "%s%d%04d", prefix, year, index);
            if (!exists.test(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Cannot generate unique code for prefix " + prefix);
    }

    private boolean hasRole(List<String> roleNames, String roleName) {
        if (roleNames == null) {
            return false;
        }
        return roleNames.stream()
                .map(this::normalizeRoleLabel)
                .anyMatch(roleName::equals);
    }

    private void updateUniqueFields(User user, UserUpdateRequest request) {
        String email = blankToNull(request.getEmail());
        if (request.getEmail() != null && !equalsNullable(user.getEmail(), email)) {
            if (email != null && userRepository.existsByEmail(email)) {
                throw new IllegalArgumentException("Email already exists");
            }
            user.setEmail(email);
        }

        String phone = blankToNull(request.getPhone());
        if (request.getPhone() != null) {
            user.setPhone(phone);
        }

        String studentId = blankToNull(request.getStudentId());
        if (request.getStudentId() != null && !equalsNullable(user.getStudentId(), studentId)) {
            if (studentId != null && userRepository.existsByStudentId(studentId)) {
                throw new IllegalArgumentException("Student ID already exists");
            }
            user.setStudentId(studentId);
        }

        String employeeCode = blankToNull(request.getEmployeeCode());
        if (request.getEmployeeCode() != null && !equalsNullable(user.getEmployeeCode(), employeeCode)) {
            if (employeeCode != null && userRepository.existsByEmployeeCode(employeeCode)) {
                throw new IllegalArgumentException("Employee code already exists");
            }
            user.setEmployeeCode(employeeCode);
        }
    }

    private void replaceRoles(User user, List<String> roleNames, String actorUsername) {
        List<String> effectiveRoleNames = roleNames == null ? Collections.emptyList() : roleNames;
        userRoleRepository.deleteByUser(user);
        user.getUserRoles().clear();

        Long actorId = userRepository.findByUsername(actorUsername)
                .map(User::getId)
                .orElse(null);

        for (String roleName : effectiveRoleNames) {
            Role role = findRole(roleName);
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            userRole.setAssignedBy(actorId);
            userRoleRepository.save(userRole);
            user.getUserRoles().add(userRole);
        }
    }

    private Role findRole(String roleName) {
        String normalized = normalizeRoleLabel(roleName);
        return roleRepository.findByName(normalized)
                .or(() -> roleRepository.findByName("ROLE_" + normalized))
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
    }

    private String normalizeRoleLabel(String roleName) {
        String normalized = roleName == null ? "" : roleName.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("ROLE_")) {
            return normalized.substring("ROLE_".length());
        }
        return normalized;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private boolean equalsNullable(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }
}
