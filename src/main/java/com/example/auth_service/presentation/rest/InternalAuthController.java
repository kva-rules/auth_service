package com.example.auth_service.presentation.rest;

import com.example.auth_service.application.dto.TokenValidationResponse;
import com.example.auth_service.application.dto.UserResponse;
import com.example.auth_service.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/auth")
@RequiredArgsConstructor
@Slf4j
public class InternalAuthController {

    private final AuthService authService;

    @PostMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validateToken(
            @RequestHeader("Authorization") String authHeader) {
        log.debug("Internal token validation request");
        String token = extractToken(authHeader);
        return ResponseEntity.ok(authService.validateToken(token));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        log.debug("Internal get user request for userId: {}", userId);
        return ResponseEntity.ok(authService.getUserById(userId));
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return authHeader;
    }
}
