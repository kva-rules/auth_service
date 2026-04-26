package com.example.auth_service.presentation.rest;

import com.example.auth_service.application.dto.TokenValidationResponse;
import com.example.auth_service.application.dto.UserResponse;
import com.example.auth_service.application.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth (Internal)", description = "Service-to-service JWT validation endpoints (not for public use)")
@RestController
@RequestMapping("/internal/auth")
@RequiredArgsConstructor
@Slf4j
public class InternalAuthController {

    private final AuthService authService;

    @Operation(summary = "Internal: validate JWT access token", description = "Used by other services to verify a Bearer token and read claims")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validation result returned"),
            @ApiResponse(responseCode = "400", description = "Missing or malformed header"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validateToken(
            @Parameter(description = "Bearer access token in Authorization header") @RequestHeader("Authorization") String authHeader) {
        log.debug("Internal token validation request");
        String token = extractToken(authHeader);
        return ResponseEntity.ok(authService.validateToken(token));
    }

    @Operation(summary = "Internal: fetch user by id", description = "Returns the auth-service user record for the given numeric user id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserResponse> getUserById(@Parameter(description = "Numeric auth-service user id") @PathVariable Long userId) {
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
