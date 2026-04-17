package com.example.auth_service.application.service;

import com.example.auth_service.application.dto.*;
import com.example.auth_service.domain.model.*;
import com.example.auth_service.infrastructure.event.AuthEventProducer;
import com.example.auth_service.infrastructure.exception.*;
import com.example.auth_service.infrastructure.persistence.AuthUserRepository;
import com.example.auth_service.infrastructure.persistence.LoginHistoryRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private LoginHistoryRepository loginHistoryRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private RoleService roleService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AuthEventProducer authEventProducer;

    @Mock
    private AuthMetricsService metricsService;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RefreshToken testRefreshToken;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "accessTokenExpiration", 3600000L);
        ReflectionTestUtils.setField(authService, "maxFailedAttempts", 5);

        testUser = User.builder()
                .authUserId(UUID.randomUUID())
                .userId(1L)
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .accountStatus(AccountStatus.ACTIVE)
                .failedLoginAttempts(0)
                .createdAt(LocalDateTime.now())
                .build();

        testRefreshToken = RefreshToken.builder()
                .tokenId(UUID.randomUUID())
                .authUserId(testUser.getAuthUserId())
                .refreshToken(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(false)
                .build();
    }

    @Test
    @DisplayName("Should register user successfully")
    void register_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .email("new@example.com")
                .password("Password1")
                .build();

        when(authUserRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(authUserRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setAuthUserId(UUID.randomUUID());
            return user;
        });
        when(tokenService.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(tokenService.generateRefreshToken(any(User.class))).thenReturn(testRefreshToken);

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo(testRefreshToken.getRefreshToken());
        verify(authEventProducer).publishUserRegistered(any(UUID.class), eq(request.getEmail()));
        verify(metricsService).incrementRegistrations();
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void register_EmailExists() {
        RegisterRequest request = RegisterRequest.builder()
                .email("existing@example.com")
                .password("Password1")
                .build();

        when(authUserRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    @DisplayName("Should login user successfully")
    void login_Success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password");

        when(authUserRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
        when(tokenService.generateAccessToken(testUser)).thenReturn("access-token");
        when(tokenService.generateRefreshToken(testUser)).thenReturn(testRefreshToken);
        when(httpRequest.getHeader("User-Agent")).thenReturn("Test-Agent");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        AuthResponse response = authService.login(request, httpRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(authEventProducer).publishUserLogin(testUser.getAuthUserId(), testUser.getEmail());
        verify(metricsService).incrementLoginSuccess();
    }

    @Test
    @DisplayName("Should throw exception for locked account")
    void login_AccountLocked() {
        testUser.setAccountStatus(AccountStatus.LOCKED);
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password");

        when(authUserRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
        when(httpRequest.getHeader("User-Agent")).thenReturn("Test-Agent");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        assertThatThrownBy(() -> authService.login(request, httpRequest))
                .isInstanceOf(AccountLockedException.class)
                .hasMessageContaining("Account is locked");
    }

    @Test
    @DisplayName("Should throw exception for invalid credentials")
    void login_InvalidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongPassword");

        when(authUserRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));
        when(httpRequest.getHeader("User-Agent")).thenReturn("Test-Agent");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        assertThatThrownBy(() -> authService.login(request, httpRequest))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(metricsService).incrementLoginFailure();
    }

    @Test
    @DisplayName("Should refresh token successfully")
    void refreshToken_Success() {
        when(tokenService.validateRefreshToken(testRefreshToken.getRefreshToken()))
                .thenReturn(Optional.of(testRefreshToken));
        when(authUserRepository.findByAuthUserId(testRefreshToken.getAuthUserId()))
                .thenReturn(Optional.of(testUser));
        when(tokenService.generateAccessToken(testUser)).thenReturn("new-access-token");
        
        RefreshToken newRefreshToken = RefreshToken.builder()
                .refreshToken("new-refresh-token")
                .build();
        when(tokenService.rotateRefreshToken(testRefreshToken, testUser)).thenReturn(newRefreshToken);

        AuthResponse response = authService.refreshToken(testRefreshToken.getRefreshToken());

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        verify(metricsService).incrementTokenRefresh();
    }

    @Test
    @DisplayName("Should throw exception for invalid refresh token")
    void refreshToken_InvalidToken() {
        when(tokenService.validateRefreshToken("invalid-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken("invalid-token"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("Should change password successfully")
    void changePassword_Success() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("oldPassword")
                .newPassword("NewPassword1")
                .build();

        when(authUserRepository.findByAuthUserId(testUser.getAuthUserId()))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(request.getCurrentPassword(), testUser.getPasswordHash()))
                .thenReturn(true);
        when(passwordEncoder.encode(request.getNewPassword())).thenReturn("newEncodedPassword");

        authService.changePassword(testUser.getAuthUserId(), request);

        verify(authUserRepository).save(testUser);
        verify(tokenService).revokeAllUserTokens(testUser.getAuthUserId());
        verify(authEventProducer).publishPasswordChanged(testUser.getAuthUserId(), testUser.getEmail());
    }

    @Test
    @DisplayName("Should throw exception for incorrect current password")
    void changePassword_IncorrectCurrentPassword() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("wrongPassword")
                .newPassword("NewPassword1")
                .build();

        when(authUserRepository.findByAuthUserId(testUser.getAuthUserId()))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(request.getCurrentPassword(), testUser.getPasswordHash()))
                .thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(testUser.getAuthUserId(), request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Current password is incorrect");
    }

    @Test
    @DisplayName("Should lock account successfully")
    void lockAccount_Success() {
        when(authUserRepository.findByUserId(testUser.getUserId()))
                .thenReturn(Optional.of(testUser));

        authService.lockAccount(testUser.getUserId());

        assertThat(testUser.getAccountStatus()).isEqualTo(AccountStatus.LOCKED);
        verify(tokenService).revokeAllUserTokens(testUser.getAuthUserId());
        verify(authEventProducer).publishAccountLocked(testUser.getAuthUserId(), testUser.getEmail());
    }

    @Test
    @DisplayName("Should unlock account successfully")
    void unlockAccount_Success() {
        testUser.setAccountStatus(AccountStatus.LOCKED);
        testUser.setFailedLoginAttempts(5);

        when(authUserRepository.findByUserId(testUser.getUserId()))
                .thenReturn(Optional.of(testUser));

        authService.unlockAccount(testUser.getUserId());

        assertThat(testUser.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(testUser.getFailedLoginAttempts()).isEqualTo(0);
        verify(authEventProducer).publishAccountUnlocked(testUser.getAuthUserId(), testUser.getEmail());
    }

    @Test
    @DisplayName("Should validate token successfully")
    void validateToken_Success() {
        when(tokenService.validateAccessToken("valid-token")).thenReturn(true);
        when(tokenService.extractUserId("valid-token")).thenReturn(1L);
        when(tokenService.extractRoles("valid-token")).thenReturn(java.util.List.of("ROLE_USER"));

        TokenValidationResponse response = authService.validateToken("valid-token");

        assertThat(response.isValid()).isTrue();
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getRoles()).contains("ROLE_USER");
    }

    @Test
    @DisplayName("Should return invalid for invalid token")
    void validateToken_Invalid() {
        when(tokenService.validateAccessToken("invalid-token")).thenReturn(false);

        TokenValidationResponse response = authService.validateToken("invalid-token");

        assertThat(response.isValid()).isFalse();
    }
}
