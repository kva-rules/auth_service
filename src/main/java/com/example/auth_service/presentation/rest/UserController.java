package com.example.auth_service.presentation.rest;

import com.example.auth_service.application.dto.ApiResponse;
import com.example.auth_service.application.dto.LoginHistoryResponse;
import com.example.auth_service.application.service.AuthService;
import com.example.auth_service.domain.model.LoginHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/{userId}")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final AuthService authService;

    @PutMapping("/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> lockAccount(@PathVariable Long userId) {
        log.info("Lock account request for userId: {}", userId);
        authService.lockAccount(userId);
        return ResponseEntity.ok(ApiResponse.success("Account locked successfully", null));
    }

    @PutMapping("/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unlockAccount(@PathVariable Long userId) {
        log.info("Unlock account request for userId: {}", userId);
        authService.unlockAccount(userId);
        return ResponseEntity.ok(ApiResponse.success("Account unlocked successfully", null));
    }

    @GetMapping("/login-history")
    @PreAuthorize("hasRole('ADMIN') or @userSecurityService.isCurrentUser(#userId)")
    public ResponseEntity<ApiResponse<Page<LoginHistoryResponse>>> getLoginHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
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
