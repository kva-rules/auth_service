package com.example.auth_service.presentation.rest;

import com.example.auth_service.application.dto.*;
import com.example.auth_service.application.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Authentication", description = "Register / login / logout / password reset / token refresh")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new user", description = "Creates a new auth-service user and returns auth tokens")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User registered"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request for email: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("User registered successfully", response));
    }

    @Operation(summary = "Log in with email and password", description = "Authenticates a user and returns JWT access/refresh tokens")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        log.info("Login request for email: {}", request.getEmail());
        AuthResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @Operation(summary = "Log out current session", description = "Revokes the provided refresh token for the authenticated user")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logged out"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody RefreshTokenRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Logout request");
        UUID authUserId = ((com.example.auth_service.domain.model.User) userDetails).getAuthUserId();
        authService.logout(request.getRefreshToken(), authUserId);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @Operation(summary = "Refresh JWT access token", description = "Exchanges a valid refresh token for a new access token pair")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid or expired refresh token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request");
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @Operation(summary = "Validate a JWT access token", description = "Checks the supplied Bearer token and returns its validity and claims")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Validation result returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing or malformed header"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validateToken(
            @Parameter(description = "Bearer access token in Authorization header") @RequestHeader("Authorization") String authHeader) {
        log.debug("Token validation request");
        String token = extractToken(authHeader);
        return ResponseEntity.ok(authService.validateToken(token));
    }

    @Operation(summary = "Change current user's password", description = "Requires current password and sets a new one for the authenticated user")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password changed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Password change request");
        UUID authUserId = ((com.example.auth_service.domain.model.User) userDetails).getAuthUserId();
        authService.changePassword(authUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    @Operation(summary = "Request password reset email", description = "Sends a reset link to the provided email if an account exists")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reset email dispatched"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Password reset request for email: {}", request.getEmail());
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset email sent", null));
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return authHeader;
    }
}
