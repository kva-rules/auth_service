package com.example.auth_service.application.service;

import com.example.auth_service.domain.model.Role;
import com.example.auth_service.domain.model.User;
import com.example.auth_service.domain.model.UserRole;
import com.example.auth_service.infrastructure.exception.ResourceNotFoundException;
import com.example.auth_service.infrastructure.exception.RoleAlreadyAssignedException;
import com.example.auth_service.infrastructure.persistence.AuthUserRepository;
import com.example.auth_service.infrastructure.persistence.RoleRepository;
import com.example.auth_service.infrastructure.persistence.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private AuthUserRepository authUserRepository;

    @InjectMocks
    private RoleService roleService;

    private User testUser;
    private Role testRole;
    private UserRole testUserRole;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .authUserId(UUID.randomUUID())
                .userId(1L)
                .email("test@example.com")
                .build();

        testRole = Role.builder()
                .roleId(UUID.randomUUID())
                .roleName("USER")
                .description("Standard user role")
                .build();

        testUserRole = UserRole.builder()
                .id(UUID.randomUUID())
                .authUserId(testUser.getAuthUserId())
                .roleId(testRole.getRoleId())
                .role(testRole)
                .build();
    }

    @Test
    @DisplayName("Should get user roles successfully")
    void getUserRoles_Success() {
        when(userRoleRepository.findByAuthUserId(testUser.getAuthUserId()))
                .thenReturn(List.of(testUserRole));

        List<Role> roles = roleService.getUserRoles(testUser.getAuthUserId());

        assertThat(roles).hasSize(1);
        assertThat(roles.get(0).getRoleName()).isEqualTo("USER");
    }

    @Test
    @DisplayName("Should get user roles by userId successfully")
    void getUserRolesByUserId_Success() {
        when(authUserRepository.findByUserId(testUser.getUserId()))
                .thenReturn(Optional.of(testUser));
        when(userRoleRepository.findByAuthUserId(testUser.getAuthUserId()))
                .thenReturn(List.of(testUserRole));

        List<Role> roles = roleService.getUserRolesByUserId(testUser.getUserId());

        assertThat(roles).hasSize(1);
    }

    @Test
    @DisplayName("Should throw exception when user not found by userId")
    void getUserRolesByUserId_UserNotFound() {
        when(authUserRepository.findByUserId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.getUserRolesByUserId(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Should assign role successfully")
    void assignRole_Success() {
        when(authUserRepository.existsByAuthUserId(testUser.getAuthUserId())).thenReturn(true);
        when(roleRepository.existsById(testRole.getRoleId())).thenReturn(true);
        when(userRoleRepository.existsByAuthUserIdAndRoleId(testUser.getAuthUserId(), testRole.getRoleId()))
                .thenReturn(false);
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserRole result = roleService.assignRole(testUser.getAuthUserId(), testRole.getRoleId());

        assertThat(result).isNotNull();
        assertThat(result.getAuthUserId()).isEqualTo(testUser.getAuthUserId());
        assertThat(result.getRoleId()).isEqualTo(testRole.getRoleId());
    }

    @Test
    @DisplayName("Should throw exception when user not found for role assignment")
    void assignRole_UserNotFound() {
        UUID unknownUserId = UUID.randomUUID();
        when(authUserRepository.existsByAuthUserId(unknownUserId)).thenReturn(false);

        assertThatThrownBy(() -> roleService.assignRole(unknownUserId, testRole.getRoleId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Should throw exception when role not found")
    void assignRole_RoleNotFound() {
        UUID unknownRoleId = UUID.randomUUID();
        when(authUserRepository.existsByAuthUserId(testUser.getAuthUserId())).thenReturn(true);
        when(roleRepository.existsById(unknownRoleId)).thenReturn(false);

        assertThatThrownBy(() -> roleService.assignRole(testUser.getAuthUserId(), unknownRoleId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Role not found");
    }

    @Test
    @DisplayName("Should throw exception when role already assigned")
    void assignRole_AlreadyAssigned() {
        when(authUserRepository.existsByAuthUserId(testUser.getAuthUserId())).thenReturn(true);
        when(roleRepository.existsById(testRole.getRoleId())).thenReturn(true);
        when(userRoleRepository.existsByAuthUserIdAndRoleId(testUser.getAuthUserId(), testRole.getRoleId()))
                .thenReturn(true);

        assertThatThrownBy(() -> roleService.assignRole(testUser.getAuthUserId(), testRole.getRoleId()))
                .isInstanceOf(RoleAlreadyAssignedException.class);
    }

    @Test
    @DisplayName("Should assign role by name successfully")
    void assignRoleByName_Success() {
        when(roleRepository.findByRoleName("USER")).thenReturn(Optional.of(testRole));
        when(authUserRepository.existsByAuthUserId(testUser.getAuthUserId())).thenReturn(true);
        when(roleRepository.existsById(testRole.getRoleId())).thenReturn(true);
        when(userRoleRepository.existsByAuthUserIdAndRoleId(testUser.getAuthUserId(), testRole.getRoleId()))
                .thenReturn(false);
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserRole result = roleService.assignRoleByName(testUser.getAuthUserId(), "USER");

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should remove role successfully")
    void removeRole_Success() {
        when(userRoleRepository.existsByAuthUserIdAndRoleId(testUser.getAuthUserId(), testRole.getRoleId()))
                .thenReturn(true);

        roleService.removeRole(testUser.getAuthUserId(), testRole.getRoleId());

        verify(userRoleRepository).deleteByAuthUserIdAndRoleId(testUser.getAuthUserId(), testRole.getRoleId());
    }

    @Test
    @DisplayName("Should throw exception when removing non-existent role assignment")
    void removeRole_NotAssigned() {
        when(userRoleRepository.existsByAuthUserIdAndRoleId(testUser.getAuthUserId(), testRole.getRoleId()))
                .thenReturn(false);

        assertThatThrownBy(() -> roleService.removeRole(testUser.getAuthUserId(), testRole.getRoleId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User does not have this role");
    }

    @Test
    @DisplayName("Should get all roles")
    void getAllRoles_Success() {
        Role adminRole = Role.builder().roleId(UUID.randomUUID()).roleName("ADMIN").build();
        when(roleRepository.findAll()).thenReturn(List.of(testRole, adminRole));

        List<Role> roles = roleService.getAllRoles();

        assertThat(roles).hasSize(2);
    }
}
