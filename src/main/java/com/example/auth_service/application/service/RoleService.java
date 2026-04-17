package com.example.auth_service.application.service;

import com.example.auth_service.domain.model.Role;
import com.example.auth_service.domain.model.User;
import com.example.auth_service.domain.model.UserRole;
import com.example.auth_service.infrastructure.exception.ResourceNotFoundException;
import com.example.auth_service.infrastructure.exception.RoleAlreadyAssignedException;
import com.example.auth_service.infrastructure.persistence.AuthUserRepository;
import com.example.auth_service.infrastructure.persistence.RoleRepository;
import com.example.auth_service.infrastructure.persistence.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final AuthUserRepository authUserRepository;

    public List<Role> getUserRoles(UUID authUserId) {
        log.debug("Getting roles for user: {}", authUserId);
        List<UserRole> userRoles = userRoleRepository.findByAuthUserId(authUserId);
        return userRoles.stream()
                .map(UserRole::getRole)
                .toList();
    }

    public List<Role> getUserRolesByUserId(Long userId) {
        log.debug("Getting roles for userId: {}", userId);
        User user = authUserRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with userId: " + userId));
        return getUserRoles(user.getAuthUserId());
    }

    @Transactional
    public UserRole assignRole(UUID authUserId, UUID roleId) {
        log.info("Assigning role {} to user {}", roleId, authUserId);
        
        if (!authUserRepository.existsByAuthUserId(authUserId)) {
            throw new ResourceNotFoundException("User not found with id: " + authUserId);
        }
        
        if (!roleRepository.existsById(roleId)) {
            throw new ResourceNotFoundException("Role not found with id: " + roleId);
        }
        
        if (userRoleRepository.existsByAuthUserIdAndRoleId(authUserId, roleId)) {
            throw new RoleAlreadyAssignedException("Role already assigned to user");
        }

        UserRole userRole = UserRole.builder()
                .authUserId(authUserId)
                .roleId(roleId)
                .build();

        return userRoleRepository.save(userRole);
    }

    @Transactional
    public UserRole assignRoleByName(UUID authUserId, String roleName) {
        log.info("Assigning role {} to user {}", roleName, authUserId);
        
        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
        
        return assignRole(authUserId, role.getRoleId());
    }

    @Transactional
    public void removeRole(UUID authUserId, UUID roleId) {
        log.info("Removing role {} from user {}", roleId, authUserId);
        
        if (!userRoleRepository.existsByAuthUserIdAndRoleId(authUserId, roleId)) {
            throw new ResourceNotFoundException("User does not have this role");
        }
        
        userRoleRepository.deleteByAuthUserIdAndRoleId(authUserId, roleId);
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public Role getRoleById(UUID roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));
    }

    public Role getRoleByName(String roleName) {
        return roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
    }
}
