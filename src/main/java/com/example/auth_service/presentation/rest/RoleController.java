package com.example.auth_service.presentation.rest;

import com.example.auth_service.application.dto.ApiResponse;
import com.example.auth_service.application.dto.AssignRoleRequest;
import com.example.auth_service.application.dto.RoleResponse;
import com.example.auth_service.application.service.RoleService;
import com.example.auth_service.domain.model.Role;
import com.example.auth_service.domain.model.User;
import com.example.auth_service.infrastructure.exception.ResourceNotFoundException;
import com.example.auth_service.infrastructure.persistence.AuthUserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Roles", description = "Role management - seed/list/update roles")
@RestController
@RequestMapping("/users/{userId}/roles")
@RequiredArgsConstructor
@Slf4j
public class RoleController {

    private final RoleService roleService;
    private final AuthUserRepository authUserRepository;

    @Operation(summary = "List roles for a user", description = "Returns all roles assigned to the specified auth-service user")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Roles returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or @userSecurityService.isCurrentUser(#userId)")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getUserRoles(@Parameter(description = "Numeric auth-service user id") @PathVariable Long userId) {
        log.info("Getting roles for userId: {}", userId);
        List<Role> roles = roleService.getUserRolesByUserId(userId);
        List<RoleResponse> roleResponses = roles.stream()
                .map(this::mapToRoleResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(roleResponses));
    }

    @Operation(summary = "Assign a role to a user", description = "Grants the named role to the specified user (admin only)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Role assigned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> assignRole(
            @Parameter(description = "Numeric auth-service user id") @PathVariable Long userId,
            @Valid @RequestBody AssignRoleRequest request) {
        log.info("Assigning role {} to userId: {}", request.getRoleName(), userId);
        UUID authUserId = getAuthUserIdByUserId(userId);
        roleService.assignRoleByName(authUserId, request.getRoleName());
        return ResponseEntity.ok(ApiResponse.success("Role assigned successfully", null));
    }

    @Operation(summary = "Remove a role from a user", description = "Revokes the specified role from the specified user (admin only)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Role removed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User or role not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeRole(
            @Parameter(description = "Numeric auth-service user id") @PathVariable Long userId,
            @Parameter(description = "Role UUID to remove") @PathVariable UUID roleId) {
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
