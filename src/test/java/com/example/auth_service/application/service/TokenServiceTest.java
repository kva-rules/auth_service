package com.example.auth_service.application.service;

import com.example.auth_service.domain.model.RefreshToken;
import com.example.auth_service.domain.model.User;
import com.example.auth_service.infrastructure.persistence.RefreshTokenRepository;
import com.example.auth_service.infrastructure.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private TokenService tokenService;

    private User testUser;
    private RefreshToken testRefreshToken;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tokenService, "refreshTokenExpiration", 86400000L);

        testUser = User.builder()
                .authUserId(UUID.randomUUID())
                .userId(1L)
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .build();

        testRefreshToken = RefreshToken.builder()
                .tokenId(UUID.randomUUID())
                .authUserId(testUser.getAuthUserId())
                .refreshToken(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should generate access token successfully")
    void generateAccessToken_Success() {
        when(jwtUtil.generateToken(any(User.class), anyMap())).thenReturn("test-jwt-token");

        String token = tokenService.generateAccessToken(testUser);

        assertThat(token).isEqualTo("test-jwt-token");
        verify(jwtUtil).generateToken(eq(testUser), anyMap());
    }

    @Test
    @DisplayName("Should generate refresh token successfully")
    void generateRefreshToken_Success() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken result = tokenService.generateRefreshToken(testUser);

        assertThat(result).isNotNull();
        assertThat(result.getAuthUserId()).isEqualTo(testUser.getAuthUserId());
        assertThat(result.getRevoked()).isFalse();
        verify(refreshTokenRepository).revokeAllByAuthUserId(testUser.getAuthUserId());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should validate access token successfully")
    void validateAccessToken_Success() {
        when(jwtUtil.validateTokenOnly("valid-token")).thenReturn(true);

        boolean result = tokenService.validateAccessToken("valid-token");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false for invalid access token")
    void validateAccessToken_Invalid() {
        when(jwtUtil.validateTokenOnly("invalid-token")).thenReturn(false);

        boolean result = tokenService.validateAccessToken("invalid-token");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should validate refresh token successfully")
    void validateRefreshToken_Success() {
        when(refreshTokenRepository.findByRefreshTokenAndRevokedFalse(testRefreshToken.getRefreshToken()))
                .thenReturn(Optional.of(testRefreshToken));

        Optional<RefreshToken> result = tokenService.validateRefreshToken(testRefreshToken.getRefreshToken());

        assertThat(result).isPresent();
        assertThat(result.get().getRefreshToken()).isEqualTo(testRefreshToken.getRefreshToken());
    }

    @Test
    @DisplayName("Should return empty for expired refresh token")
    void validateRefreshToken_Expired() {
        RefreshToken expiredToken = RefreshToken.builder()
                .refreshToken("expired-token")
                .expiresAt(LocalDateTime.now().minusDays(1))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByRefreshTokenAndRevokedFalse("expired-token"))
                .thenReturn(Optional.of(expiredToken));

        Optional<RefreshToken> result = tokenService.validateRefreshToken("expired-token");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should revoke all user tokens")
    void revokeAllUserTokens_Success() {
        UUID authUserId = testUser.getAuthUserId();

        tokenService.revokeAllUserTokens(authUserId);

        verify(refreshTokenRepository).revokeAllByAuthUserId(authUserId);
    }

    @Test
    @DisplayName("Should revoke single token")
    void revokeToken_Success() {
        when(refreshTokenRepository.findByRefreshTokenAndRevokedFalse(testRefreshToken.getRefreshToken()))
                .thenReturn(Optional.of(testRefreshToken));

        tokenService.revokeToken(testRefreshToken.getRefreshToken());

        assertThat(testRefreshToken.getRevoked()).isTrue();
        verify(refreshTokenRepository).save(testRefreshToken);
    }

    @Test
    @DisplayName("Should rotate refresh token")
    void rotateRefreshToken_Success() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken newToken = tokenService.rotateRefreshToken(testRefreshToken, testUser);

        assertThat(testRefreshToken.getRevoked()).isTrue();
        assertThat(newToken).isNotNull();
        assertThat(newToken.getAuthUserId()).isEqualTo(testUser.getAuthUserId());
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }
}
