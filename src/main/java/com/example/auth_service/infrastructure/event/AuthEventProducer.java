package com.example.auth_service.infrastructure.event;

import com.example.auth_service.domain.event.AuthEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.user-events}")
    private String userEventsTopic;

    public void publishUserRegistered(UUID authUserId, String email) {
        AuthEvent event = AuthEvent.builder()
                .eventType("user.registered")
                .authUserId(authUserId)
                .email(email)
                .timestamp(LocalDateTime.now())
                .build();
        sendEvent(event);
        log.info("Published user.registered event for user: {}", email);
    }

    public void publishUserLogin(UUID authUserId, String email) {
        AuthEvent event = AuthEvent.builder()
                .eventType("user.login")
                .authUserId(authUserId)
                .email(email)
                .timestamp(LocalDateTime.now())
                .build();
        sendEvent(event);
        log.info("Published user.login event for user: {}", email);
    }

    public void publishUserLogout(UUID authUserId, String email) {
        AuthEvent event = AuthEvent.builder()
                .eventType("user.logout")
                .authUserId(authUserId)
                .email(email)
                .timestamp(LocalDateTime.now())
                .build();
        sendEvent(event);
        log.info("Published user.logout event for user: {}", email);
    }

    public void publishPasswordChanged(UUID authUserId, String email) {
        AuthEvent event = AuthEvent.builder()
                .eventType("user.password.changed")
                .authUserId(authUserId)
                .email(email)
                .timestamp(LocalDateTime.now())
                .build();
        sendEvent(event);
        log.info("Published user.password.changed event for user: {}", email);
    }

    public void publishPasswordResetRequested(UUID authUserId, String email) {
        AuthEvent event = AuthEvent.builder()
                .eventType("user.password.reset.requested")
                .authUserId(authUserId)
                .email(email)
                .timestamp(LocalDateTime.now())
                .build();
        sendEvent(event);
        log.info("Published user.password.reset.requested event for user: {}", email);
    }

    public void publishAccountLocked(UUID authUserId, String email) {
        AuthEvent event = AuthEvent.builder()
                .eventType("user.account.locked")
                .authUserId(authUserId)
                .email(email)
                .timestamp(LocalDateTime.now())
                .build();
        sendEvent(event);
        log.info("Published user.account.locked event for user: {}", email);
    }

    public void publishAccountUnlocked(UUID authUserId, String email) {
        AuthEvent event = AuthEvent.builder()
                .eventType("user.account.unlocked")
                .authUserId(authUserId)
                .email(email)
                .timestamp(LocalDateTime.now())
                .build();
        sendEvent(event);
        log.info("Published user.account.unlocked event for user: {}", email);
    }

    private void sendEvent(AuthEvent event) {
        try {
            kafkaTemplate.send(userEventsTopic, event.getAuthUserId().toString(), event);
        } catch (Exception e) {
            log.error("Failed to send event: {}", event.getEventType(), e);
        }
    }
}
