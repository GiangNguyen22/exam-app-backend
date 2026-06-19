package com.android.app.exam_app_backend.security;

import com.android.app.exam_app_backend.entity.User;
import com.android.app.exam_app_backend.entity.UserRole;
import com.android.app.exam_app_backend.entity.enums.UserStatus;
import com.android.app.exam_app_backend.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameWithRolesAndPermissions(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        Set<GrantedAuthority> authorities = new HashSet<>();

        Set<SimpleGrantedAuthority> roleAuthorities = user.getUserRoles().stream()
                .map(UserRole::getRole)
                .map(role -> role.getName().startsWith("ROLE_") ? role.getName() : "ROLE_" + role.getName())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
        authorities.addAll(roleAuthorities);

        Set<SimpleGrantedAuthority> permissionAuthorities = user.getUserRoles().stream()
                .flatMap(userRole -> userRole.getRole().getPermissions().stream())
                .map(permission -> new SimpleGrantedAuthority(permission.getName()))
                .collect(Collectors.toSet());
        authorities.addAll(permissionAuthorities);

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .accountLocked(user.getStatus() == UserStatus.LOCKED)
                .disabled(user.getStatus() == UserStatus.PENDING)
                .build();
    }
}

