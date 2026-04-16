package com.example.auth_service.infrastructure.event;

import com.example.auth_service.domain.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.user-events}")
    private String topic;

    public void sendUserRegisteredEvent(UserRegisteredEvent event) {
        kafkaTemplate.send(topic, event.getEmail(), event);
    }
}
