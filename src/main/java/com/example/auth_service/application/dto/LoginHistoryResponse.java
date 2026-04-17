package com.example.auth_service.application.dto;

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
public class LoginHistoryResponse {
    private UUID loginId;
    private LocalDateTime loginTime;
    private String ipAddress;
    private String deviceInfo;
    private String status;
}
