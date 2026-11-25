package com.android.app.exam_app_backend.service;

import com.android.app.exam_app_backend.entity.User;
import com.android.app.exam_app_backend.payload.LoginRequest;
import com.android.app.exam_app_backend.payload.LoginResponse;
import com.android.app.exam_app_backend.payload.RefreshTokenRequest;
import com.android.app.exam_app_backend.payload.TokenRefreshResponse;
import com.android.app.exam_app_backend.repository.UserRepository;
import com.android.app.exam_app_backend.security.JwtTokenProvider;
import com.android.app.exam_app_backend.security.TokenRevocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final TokenRevocationService tokenRevocationService;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       JwtTokenProvider tokenProvider,
                       RefreshTokenService refreshTokenService,
                       TokenRevocationService tokenRevocationService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.tokenRevocationService = tokenRevocationService;
    }

    @Transactional(noRollbackFor = {AuthenticationException.class})
    public LoginResponse login(LoginRequest loginRequest) {
        String username = loginRequest.getUsername();
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getLockedUntil() != null) {
                if (user.getLockedUntil().isAfter(LocalDateTime.now())) {
                    long minutesLeft = Math.max(1, Duration.between(LocalDateTime.now(), user.getLockedUntil()).toMinutes() + 1);
                    log.warn("Login attempt on locked account username={} lockedUntil={}", username, user.getLockedUntil());
                    throw new LockedException("Tài khoản của bạn đang bị khóa do nhập sai mật khẩu quá 5 lần. Vui lòng thử lại sau " + minutesLeft + " phút.");
                } else {
                    // Lock time has expired, reset
                    user.setFailedLoginAttempts(0);
                    user.setLockedUntil(null);
                    userRepository.save(user);
                }
            }
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword())
            );
        } catch (AuthenticationException ex) {
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                int currentAttempts = (user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0) + 1;
                user.setFailedLoginAttempts(currentAttempts);

                if (currentAttempts >= MAX_FAILED_ATTEMPTS) {
                    user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
                    userRepository.save(user);
                    log.warn("Account locked due to brute-force attempts username={}", username);
                    throw new LockedException("Tài khoản của bạn đã bị khóa 15 phút do nhập sai mật khẩu 5 lần liên tiếp.");
                }

                userRepository.save(user);
                int remaining = MAX_FAILED_ATTEMPTS - currentAttempts;
                throw new BadCredentialsException("Tên đăng nhập hoặc mật khẩu không chính xác. Bạn còn " + remaining + " lần thử.");
            }
            throw ex;
        }

        // On successful authentication, reset lockout counters
        User authenticatedUser = userOpt.orElseGet(() -> userRepository.findByUsername(username).orElse(null));
        if (authenticatedUser != null) {
            authenticatedUser.setFailedLoginAttempts(0);
            authenticatedUser.setLockedUntil(null);
            userRepository.save(authenticatedUser);
        }

        String jwt = tokenProvider.generateToken(authentication);
        String refreshToken = authenticatedUser != null ? refreshTokenService.createRefreshToken(authenticatedUser) : null;

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .collect(Collectors.toList());

        log.info("User logged in successfully username={} roles={}", username, roles);
        return new LoginResponse(jwt, refreshToken, roles);
    }

    public TokenRefreshResponse refresh(RefreshTokenRequest request) {
        return refreshTokenService.rotateRefreshToken(request.getRefreshToken());
    }

    public void logout(String bearerToken, String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.trim().isEmpty()) {
            refreshTokenService.revokeToken(rawRefreshToken.trim());
        }

        if (bearerToken != null && !bearerToken.trim().isEmpty()) {
            String jwt = bearerToken.startsWith("Bearer ") ? bearerToken.substring(7) : bearerToken;
            if (tokenProvider.validateToken(jwt)) {
                Date expiration = tokenProvider.getExpirationDateFromJWT(jwt);
                tokenRevocationService.revokeToken(jwt, expiration);
            }
        }
        log.info("User logged out and tokens invalidated");
    }
}
