package com.example.auth_service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenValidationResponse {
    private boolean valid;
    // Legacy auto-increment id (always 0 in this build — kept for backwards compatibility
    // with any old client that reads it). Real identity is in authUserId.
    private Long userId;
    // The stable UUID identity of the user. Downstream services (knowledge, reward,
    // notification) inject this via X-User-Id and parse it as a UUID — so the gateway
    // MUST surface this field, not the legacy Long userId.
    private UUID authUserId;
    private List<String> roles;
}
