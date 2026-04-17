package com.example.auth_service.application.service;

import com.example.auth_service.domain.model.RefreshToken;
import com.example.auth_service.domain.model.User;
import com.example.auth_service.infrastructure.persistence.RefreshTokenRepository;
import com.example.auth_service.infrastructure.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpiration;

    public String generateAccessToken(User user) {
        log.debug("Generating access token for user: {}", user.getEmail());
        Map<String, Object> claims = Map.of(
                "userId", user.getUserId() != null ? user.getUserId() : 0,
                "authUserId", user.getAuthUserId().toString(),
                "roles", user.getAuthorities().stream()
                        .map(Object::toString)
                        .toList()
        );
        return jwtUtil.generateToken(user, claims);
    }

    @Transactional
    public RefreshToken generateRefreshToken(User user) {
        log.debug("Generating refresh token for user: {}", user.getEmail());
        
        // Revoke existing refresh tokens for this user
        revokeAllUserTokens(user.getAuthUserId());

        RefreshToken refreshToken = RefreshToken.builder()
                .authUserId(user.getAuthUserId())
                .refreshToken(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public boolean validateAccessToken(String token) {
        try {
            return jwtUtil.validateTokenOnly(token);
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public Optional<RefreshToken> validateRefreshToken(String token) {
        return refreshTokenRepository.findByRefreshTokenAndRevokedFalse(token)
                .filter(rt -> rt.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    public String extractUsername(String token) {
        return jwtUtil.extractUsername(token);
    }

    public UUID extractAuthUserId(String token) {
        Claims claims = jwtUtil.extractAllClaims(token);
        String authUserIdStr = claims.get("authUserId", String.class);
        return UUID.fromString(authUserIdStr);
    }

    public Long extractUserId(String token) {
        Claims claims = jwtUtil.extractAllClaims(token);
        return claims.get("userId", Long.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Claims claims = jwtUtil.extractAllClaims(token);
        return claims.get("roles", List.class);
    }

    public Claims extractAllClaims(String token) {
        return jwtUtil.extractAllClaims(token);
    }

    @Transactional
    public void revokeAllUserTokens(UUID authUserId) {
        log.debug("Revoking all tokens for user: {}", authUserId);
        refreshTokenRepository.revokeAllByAuthUserId(authUserId);
    }

    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.findByRefreshTokenAndRevokedFalse(token)
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                });
    }

    @Transactional
    public RefreshToken rotateRefreshToken(RefreshToken oldToken, User user) {
        log.debug("Rotating refresh token for user: {}", user.getEmail());
        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);
        return generateRefreshToken(user);
    }
}
