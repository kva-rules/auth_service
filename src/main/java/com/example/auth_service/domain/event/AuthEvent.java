package com.example.auth_service.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthEvent {
    private String eventType;
    private UUID authUserId;
    private Long userId;
    private String email;
    private LocalDateTime timestamp;
    private String additionalInfo;
}
