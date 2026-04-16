package com.example.auth_service.presentation.rest;

import com.example.auth_service.application.dto.*;
import com.example.auth_service.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/validate-token")
    public ResponseEntity<TokenValidationResponse> validateToken(@RequestHeader("Authorization") String authHeader) {
        // Extract token and validate logic here
        // For now, placeholder
        TokenValidationResponse response = TokenValidationResponse.builder()
                .valid(true)
                .userId(1L)
                .roles(List.of("USER"))
                .build();
        return ResponseEntity.ok(response);
    }
}
