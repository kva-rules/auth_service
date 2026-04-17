package com.example.auth_service.infrastructure.security;

import com.example.auth_service.domain.model.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "testSecretKeyForJWTThatIsLongEnoughForHS512SignatureInTests");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000);

        testUser = User.builder()
                .authUserId(UUID.randomUUID())
                .userId(1L)
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .build();
    }

    @Test
    @DisplayName("Should generate token successfully")
    void generateToken_Success() {
        String token = jwtUtil.generateToken(testUser);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    @DisplayName("Should generate token with extra claims")
    void generateTokenWithClaims_Success() {
        Map<String, Object> claims = Map.of(
                "userId", 1L,
                "role", "USER"
        );

        String token = jwtUtil.generateToken(testUser, claims);

        assertThat(token).isNotNull();
        Claims extractedClaims = jwtUtil.extractAllClaims(token);
        assertThat(extractedClaims.get("userId", Long.class)).isEqualTo(1L);
        assertThat(extractedClaims.get("role", String.class)).isEqualTo("USER");
    }

    @Test
    @DisplayName("Should extract username from token")
    void extractUsername_Success() {
        String token = jwtUtil.generateToken(testUser);

        String username = jwtUtil.extractUsername(token);

        assertThat(username).isEqualTo(testUser.getEmail());
    }

    @Test
    @DisplayName("Should extract expiration from token")
    void extractExpiration_Success() {
        String token = jwtUtil.generateToken(testUser);

        Date expiration = jwtUtil.extractExpiration(token);

        assertThat(expiration).isAfter(new Date());
    }

    @Test
    @DisplayName("Should validate token successfully")
    void validateToken_Success() {
        String token = jwtUtil.generateToken(testUser);

        Boolean isValid = jwtUtil.validateToken(token, testUser);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should return false for token with wrong user")
    void validateToken_WrongUser() {
        String token = jwtUtil.generateToken(testUser);

        User otherUser = User.builder()
                .email("other@example.com")
                .build();

        Boolean isValid = jwtUtil.validateToken(token, otherUser);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should validate token only (without user details)")
    void validateTokenOnly_Success() {
        String token = jwtUtil.generateToken(testUser);

        Boolean isValid = jwtUtil.validateTokenOnly(token);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should return false for invalid token")
    void validateTokenOnly_InvalidToken() {
        Boolean isValid = jwtUtil.validateTokenOnly("invalid-token");

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should return false for expired token")
    void validateTokenOnly_ExpiredToken() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000);
        String expiredToken = jwtUtil.generateToken(testUser);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000);

        Boolean isValid = jwtUtil.validateTokenOnly(expiredToken);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should extract all claims from token")
    void extractAllClaims_Success() {
        Map<String, Object> extraClaims = Map.of("customClaim", "customValue");
        String token = jwtUtil.generateToken(testUser, extraClaims);

        Claims claims = jwtUtil.extractAllClaims(token);

        assertThat(claims.getSubject()).isEqualTo(testUser.getEmail());
        assertThat(claims.get("customClaim", String.class)).isEqualTo("customValue");
    }
}
