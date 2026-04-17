package com.example.auth_service.infrastructure.event;

import com.example.auth_service.domain.event.AuthEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private AuthEventProducer authEventProducer;

    @Captor
    private ArgumentCaptor<AuthEvent> eventCaptor;

    private UUID testAuthUserId;
    private String testEmail;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authEventProducer, "userEventsTopic", "user-events");
        testAuthUserId = UUID.randomUUID();
        testEmail = "test@example.com";
    }

    @Test
    @DisplayName("Should publish user registered event")
    void publishUserRegistered_Success() {
        authEventProducer.publishUserRegistered(testAuthUserId, testEmail);

        verify(kafkaTemplate).send(eq("user-events"), eq(testAuthUserId.toString()), eventCaptor.capture());
        
        AuthEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getEventType()).isEqualTo("user.registered");
        assertThat(capturedEvent.getAuthUserId()).isEqualTo(testAuthUserId);
        assertThat(capturedEvent.getEmail()).isEqualTo(testEmail);
    }

    @Test
    @DisplayName("Should publish user login event")
    void publishUserLogin_Success() {
        authEventProducer.publishUserLogin(testAuthUserId, testEmail);

        verify(kafkaTemplate).send(eq("user-events"), eq(testAuthUserId.toString()), eventCaptor.capture());
        
        AuthEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getEventType()).isEqualTo("user.login");
    }

    @Test
    @DisplayName("Should publish user logout event")
    void publishUserLogout_Success() {
        authEventProducer.publishUserLogout(testAuthUserId, testEmail);

        verify(kafkaTemplate).send(eq("user-events"), eq(testAuthUserId.toString()), eventCaptor.capture());
        
        AuthEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getEventType()).isEqualTo("user.logout");
    }

    @Test
    @DisplayName("Should publish password changed event")
    void publishPasswordChanged_Success() {
        authEventProducer.publishPasswordChanged(testAuthUserId, testEmail);

        verify(kafkaTemplate).send(eq("user-events"), eq(testAuthUserId.toString()), eventCaptor.capture());
        
        AuthEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getEventType()).isEqualTo("user.password.changed");
    }

    @Test
    @DisplayName("Should publish account locked event")
    void publishAccountLocked_Success() {
        authEventProducer.publishAccountLocked(testAuthUserId, testEmail);

        verify(kafkaTemplate).send(eq("user-events"), eq(testAuthUserId.toString()), eventCaptor.capture());
        
        AuthEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getEventType()).isEqualTo("user.account.locked");
    }

    @Test
    @DisplayName("Should publish account unlocked event")
    void publishAccountUnlocked_Success() {
        authEventProducer.publishAccountUnlocked(testAuthUserId, testEmail);

        verify(kafkaTemplate).send(eq("user-events"), eq(testAuthUserId.toString()), eventCaptor.capture());
        
        AuthEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getEventType()).isEqualTo("user.account.unlocked");
    }
}
