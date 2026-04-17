package com.example.auth_service.infrastructure.persistence;

import com.example.auth_service.domain.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByRefreshTokenAndRevokedFalse(String refreshToken);
    
    Optional<RefreshToken> findByAuthUserIdAndRevokedFalseAndExpiresAtAfter(UUID authUserId, LocalDateTime now);
    
    List<RefreshToken> findByAuthUserIdAndRevokedFalse(UUID authUserId);
    
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.authUserId = :authUserId AND rt.revoked = false")
    void revokeAllByAuthUserId(@Param("authUserId") UUID authUserId);
    
    void deleteByAuthUserId(UUID authUserId);
}
