package com.example.auth_service.presentation.rest;

import com.example.auth_service.application.dto.ApiResponse;
import com.example.auth_service.application.dto.LoginHistoryResponse;
import com.example.auth_service.application.service.AuthService;
import com.example.auth_service.domain.model.LoginHistory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User Admin", description = "Admin operations on auth-service user accounts (distinct from User Service profiles)")
@RestController
@RequestMapping("/users/{userId}")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final AuthService authService;

    @Operation(summary = "Lock a user account", description = "Disables login for the specified user (admin only)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account locked"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> lockAccount(@Parameter(description = "Numeric auth-service user id") @PathVariable Long userId) {
        log.info("Lock account request for userId: {}", userId);
        authService.lockAccount(userId);
        return ResponseEntity.ok(ApiResponse.success("Account locked successfully", null));
    }

    @Operation(summary = "Unlock a user account", description = "Re-enables login for the specified user (admin only)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account unlocked"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unlockAccount(@Parameter(description = "Numeric auth-service user id") @PathVariable Long userId) {
        log.info("Unlock account request for userId: {}", userId);
        authService.unlockAccount(userId);
        return ResponseEntity.ok(ApiResponse.success("Account unlocked successfully", null));
    }

    @Operation(summary = "Get login history for a user", description = "Returns a paginated login history record for the specified user")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login history page returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/login-history")
    @PreAuthorize("hasRole('ADMIN') or @userSecurityService.isCurrentUser(#userId)")
    public ResponseEntity<ApiResponse<Page<LoginHistoryResponse>>> getLoginHistory(
            @Parameter(description = "Numeric auth-service user id") @PathVariable Long userId,
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        log.info("Getting login history for userId: {}", userId);
        Pageable pageable = PageRequest.of(page, size);
        Page<LoginHistory> loginHistoryPage = authService.getLoginHistory(userId, pageable);
        Page<LoginHistoryResponse> responsePage = loginHistoryPage.map(this::mapToLoginHistoryResponse);
        return ResponseEntity.ok(ApiResponse.success(responsePage));
    }

    private LoginHistoryResponse mapToLoginHistoryResponse(LoginHistory history) {
        return LoginHistoryResponse.builder()
                .loginId(history.getLoginId())
                .loginTime(history.getLoginTime())
                .ipAddress(history.getIpAddress())
                .deviceInfo(history.getDeviceInfo())
                .status(history.getStatus().name())
                .build();
    }
}
