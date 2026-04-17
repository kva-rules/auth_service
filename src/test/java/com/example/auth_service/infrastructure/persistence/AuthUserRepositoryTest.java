package com.example.auth_service.infrastructure.persistence;

import com.example.auth_service.domain.model.AccountStatus;
import com.example.auth_service.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AuthUserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AuthUserRepository authUserRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .authUserId(UUID.randomUUID())
                .userId(1L)
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .accountStatus(AccountStatus.ACTIVE)
                .failedLoginAttempts(0)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(testUser);
        entityManager.flush();
    }

    @Test
    @DisplayName("Should find user by email")
    void findByEmail_Success() {
        Optional<User> found = authUserRepository.findByEmail("test@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should return empty when email not found")
    void findByEmail_NotFound() {
        Optional<User> found = authUserRepository.findByEmail("nonexistent@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find user by userId")
    void findByUserId_Success() {
        Optional<User> found = authUserRepository.findByUserId(1L);

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should find user by authUserId")
    void findByAuthUserId_Success() {
        Optional<User> found = authUserRepository.findByAuthUserId(testUser.getAuthUserId());

        assertThat(found).isPresent();
        assertThat(found.get().getAuthUserId()).isEqualTo(testUser.getAuthUserId());
    }

    @Test
    @DisplayName("Should check if email exists")
    void existsByEmail_True() {
        boolean exists = authUserRepository.existsByEmail("test@example.com");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when email does not exist")
    void existsByEmail_False() {
        boolean exists = authUserRepository.existsByEmail("nonexistent@example.com");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should check if authUserId exists")
    void existsByAuthUserId_True() {
        boolean exists = authUserRepository.existsByAuthUserId(testUser.getAuthUserId());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when authUserId does not exist")
    void existsByAuthUserId_False() {
        boolean exists = authUserRepository.existsByAuthUserId(UUID.randomUUID());

        assertThat(exists).isFalse();
    }
}
