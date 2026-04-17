package com.example.auth_service.infrastructure.persistence;

import com.example.auth_service.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthUserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUserId(Long userId);
    Optional<User> findByAuthUserId(UUID authUserId);
    boolean existsByEmail(String email);
    boolean existsByAuthUserId(UUID authUserId);
}
