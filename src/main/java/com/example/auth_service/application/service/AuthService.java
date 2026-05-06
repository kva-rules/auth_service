package com.example.auth_service.application.service;

import com.example.auth_service.application.dto.*;
import com.example.auth_service.domain.model.*;
import com.example.auth_service.infrastructure.event.AuthEventProducer;
import com.example.auth_service.infrastructure.exception.*;
import com.example.auth_service.infrastructure.persistence.AuthUserRepository;
import com.example.auth_service.infrastructure.persistence.LoginHistoryRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthUserRepository authUserRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RoleService roleService;
    private final AuthenticationManager authenticationManager;
    private final AuthEventProducer authEventProducer;
    private final AuthMetricsService metricsService;

    @Value("${jwt.expiration}")
    private long accessTokenExpiration;

    @Value("${auth.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());
        
        if (authUserRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists: " + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .accountStatus(AccountStatus.ACTIVE)
                .failedLoginAttempts(0)
                .createdAt(LocalDateTime.now())
                .build();

        user = authUserRepository.save(user);

        // Assign default role
        String roleName = request.getRole() != null ? request.getRole() : "USER";
        try {
            roleService.assignRoleByName(user.getAuthUserId(), roleName);
        } catch (ResourceNotFoundException e) {
            log.warn("Role {} not found, skipping role assignment", roleName);
        }

        // Generate tokens
        String accessToken = tokenService.generateAccessToken(user);
        RefreshToken refreshToken = tokenService.generateRefreshToken(user);

        // Publish event
        authEventProducer.publishUserRegistered(user.getAuthUserId(), user.getEmail());
        metricsService.incrementRegistrations();

        log.info("User registered successfully: {}", user.getEmail());
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getRefreshToken())
                .expiresIn(accessTokenExpiration / 1000)
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        log.info("Login attempt for user: {}", request.getEmail());
        
        User user = authUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check account status
        if (user.getAccountStatus() == AccountStatus.LOCKED) {
            logLoginAttempt(user, httpRequest, LoginStatus.FAILED);
            throw new AccountLockedException("Account is locked. Please contact support.");
        }

        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            logLoginAttempt(user, httpRequest, LoginStatus.FAILED);
            throw new AccountLockedException("Account is suspended.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            handleFailedLogin(user, httpRequest);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // Reset failed attempts on successful login
        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(LocalDateTime.now());
        authUserRepository.save(user);

        // Generate tokens
        String accessToken = tokenService.generateAccessToken(user);
        RefreshToken refreshToken = tokenService.generateRefreshToken(user);

        // Log successful login
        logLoginAttempt(user, httpRequest, LoginStatus.SUCCESS);

        // Publish event
        authEventProducer.publishUserLogin(user.getAuthUserId(), user.getEmail());
        metricsService.incrementLoginSuccess();

        log.info("User logged in successfully: {}", user.getEmail());
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getRefreshToken())
                .expiresIn(accessTokenExpiration / 1000)
                .build();
    }

    @Transactional
    public void logout(String refreshToken, UUID authUserId) {
        log.info("Logout for user: {}", authUserId);
        
        tokenService.revokeToken(refreshToken);
        
        User user = authUserRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        authEventProducer.publishUserLogout(authUserId, user.getEmail());
        log.info("User logged out successfully: {}", authUserId);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenValue) {
        log.debug("Refreshing token");
        
        RefreshToken refreshToken = tokenService.validateRefreshToken(refreshTokenValue)
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired refresh token"));

        User user = authUserRepository.findByAuthUserId(refreshToken.getAuthUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new AccountLockedException("Account is not active");
        }

        // Generate new tokens (rotate refresh token)
        String newAccessToken = tokenService.generateAccessToken(user);
        RefreshToken newRefreshToken = tokenService.rotateRefreshToken(refreshToken, user);

        metricsService.incrementTokenRefresh();

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getRefreshToken())
                .expiresIn(accessTokenExpiration / 1000)
                .build();
    }

    public TokenValidationResponse validateToken(String token) {
        log.debug("Validating token");
        
        if (!tokenService.validateAccessToken(token)) {
            return TokenValidationResponse.builder()
                    .valid(false)
                    .build();
        }

        try {
            Long userId = tokenService.extractUserId(token);
            // The downstream services key on UUID, not the legacy Long. We surface BOTH so
            // the gateway can pick whichever matches the downstream service's expectation —
            // in practice the AuthFilter reads authUserId for X-User-Id.
            UUID authUserId = tokenService.extractAuthUserId(token);
            List<String> roles = tokenService.extractRoles(token);

            return TokenValidationResponse.builder()
                    .valid(true)
                    .userId(userId)
                    .authUserId(authUserId)
                    .roles(roles)
                    .build();
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return TokenValidationResponse.builder()
                    .valid(false)
                    .build();
        }
    }

    @Transactional
    public void changePassword(UUID authUserId, ChangePasswordRequest request) {
        log.info("Changing password for user: {}", authUserId);
        
        User user = authUserRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        authUserRepository.save(user);

        // Revoke all existing tokens
        tokenService.revokeAllUserTokens(authUserId);

        authEventProducer.publishPasswordChanged(authUserId, user.getEmail());
        log.info("Password changed successfully for user: {}", authUserId);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Password reset requested for email: {}", request.getEmail());
        
        User user = authUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // In a real implementation, this would send an email with a reset link
        // For now, we just log and publish an event
        authEventProducer.publishPasswordResetRequested(user.getAuthUserId(), user.getEmail());
        log.info("Password reset email sent to: {}", request.getEmail());
    }

    @Transactional
    public void lockAccount(UUID authUserId) {
        log.info("Locking account for authUserId: {}", authUserId);

        User user = authUserRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with authUserId: " + authUserId));

        user.setAccountStatus(AccountStatus.LOCKED);
        user.setUpdatedAt(LocalDateTime.now());
        authUserRepository.save(user);

        // Revoke all tokens
        tokenService.revokeAllUserTokens(user.getAuthUserId());

        authEventProducer.publishAccountLocked(user.getAuthUserId(), user.getEmail());
        log.info("Account locked for user: {}", user.getEmail());
    }

    @Transactional
    public void unlockAccount(UUID authUserId) {
        log.info("Unlocking account for authUserId: {}", authUserId);

        User user = authUserRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with authUserId: " + authUserId));

        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setFailedLoginAttempts(0);
        user.setUpdatedAt(LocalDateTime.now());
        authUserRepository.save(user);

        authEventProducer.publishAccountUnlocked(user.getAuthUserId(), user.getEmail());
        log.info("Account unlocked for user: {}", user.getEmail());
    }

    public Page<LoginHistory> getLoginHistory(UUID authUserId, Pageable pageable) {
        User user = authUserRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with authUserId: " + authUserId));

        return loginHistoryRepository.findByAuthUserIdOrderByLoginTimeDesc(user.getAuthUserId(), pageable);
    }

    public UserResponse getUserById(Long userId) {
        User user = authUserRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with userId: " + userId));
        
        return mapToUserResponse(user);
    }

    public UserResponse getUserByAuthUserId(UUID authUserId) {
        User user = authUserRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        return mapToUserResponse(user);
    }

    private void handleFailedLogin(User user, HttpServletRequest httpRequest) {
        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
        
        if (user.getFailedLoginAttempts() >= maxFailedAttempts) {
            user.setAccountStatus(AccountStatus.LOCKED);
            authEventProducer.publishAccountLocked(user.getAuthUserId(), user.getEmail());
            log.warn("Account locked due to too many failed attempts: {}", user.getEmail());
        }
        
        authUserRepository.save(user);
        logLoginAttempt(user, httpRequest, LoginStatus.FAILED);
        metricsService.incrementLoginFailure();
    }

    private void logLoginAttempt(User user, HttpServletRequest request, LoginStatus status) {
        LoginHistory loginHistory = LoginHistory.builder()
                .authUserId(user.getAuthUserId())
                .loginTime(LocalDateTime.now())
                .ipAddress(getClientIp(request))
                .deviceInfo(request.getHeader("User-Agent"))
                .status(status)
                .build();
        
        loginHistoryRepository.save(loginHistory);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private UserResponse mapToUserResponse(User user) {
        List<String> roles = user.getUserRoles() != null 
                ? user.getUserRoles().stream()
                    .map(ur -> ur.getRole().getRoleName())
                    .toList()
                : List.of();

        return UserResponse.builder()
                .authUserId(user.getAuthUserId())
                .userId(user.getUserId())
                .email(user.getEmail())
                .accountStatus(user.getAccountStatus().name())
                .roles(roles)
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
