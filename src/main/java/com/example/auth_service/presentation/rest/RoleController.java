package com.example.auth_service.presentation.rest;

import com.example.auth_service.application.dto.ApiResponse;
import com.example.auth_service.application.dto.AssignRoleRequest;
import com.example.auth_service.application.dto.RoleResponse;
import com.example.auth_service.application.service.RoleService;
import com.example.auth_service.domain.model.Role;
import com.example.auth_service.domain.model.User;
import com.example.auth_service.infrastructure.exception.ResourceNotFoundException;
import com.example.auth_service.infrastructure.persistence.AuthUserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users/{userId}/roles")
@RequiredArgsConstructor
@Slf4j
public class RoleController {

    private final RoleService roleService;
    private final AuthUserRepository authUserRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or @userSecurityService.isCurrentUser(#userId)")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getUserRoles(@PathVariable Long userId) {
        log.info("Getting roles for userId: {}", userId);
        List<Role> roles = roleService.getUserRolesByUserId(userId);
        List<RoleResponse> roleResponses = roles.stream()
                .map(this::mapToRoleResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(roleResponses));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> assignRole(
            @PathVariable Long userId,
            @Valid @RequestBody AssignRoleRequest request) {
        log.info("Assigning role {} to userId: {}", request.getRoleName(), userId);
        UUID authUserId = getAuthUserIdByUserId(userId);
        roleService.assignRoleByName(authUserId, request.getRoleName());
        return ResponseEntity.ok(ApiResponse.success("Role assigned successfully", null));
    }

    @DeleteMapping("/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeRole(
            @PathVariable Long userId,
            @PathVariable UUID roleId) {
        log.info("Removing role {} from userId: {}", roleId, userId);
        UUID authUserId = getAuthUserIdByUserId(userId);
        roleService.removeRole(authUserId, roleId);
        return ResponseEntity.ok(ApiResponse.success("Role removed successfully", null));
    }

    private RoleResponse mapToRoleResponse(Role role) {
        return RoleResponse.builder()
                .roleId(role.getRoleId())
                .roleName(role.getRoleName())
                .description(role.getDescription())
                .build();
    }

    private UUID getAuthUserIdByUserId(Long userId) {
        User user = authUserRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with userId: " + userId));
        return user.getAuthUserId();
    }
}
