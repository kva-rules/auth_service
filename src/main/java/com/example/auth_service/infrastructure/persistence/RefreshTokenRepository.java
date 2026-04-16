package com.example.auth_service.infrastructure.persistence;

import com.example.auth_service.domain.model.RefreshToken;
import com.example.auth_service.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenValueAndRevokedFalseAndExpiresAtAfter(String tokenValue, LocalDateTime now);
    Optional<RefreshToken> findByUserAndRevokedFalseAndExpiresAtAfter(User user, LocalDateTime now);
    void deleteByUser(User user);
}
