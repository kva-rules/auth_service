package com.example.auth_service.infrastructure.persistence;

import com.example.auth_service.domain.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {
    List<UserRole> findByAuthUserId(UUID authUserId);
    
    Optional<UserRole> findByAuthUserIdAndRoleId(UUID authUserId, UUID roleId);
    
    void deleteByAuthUserIdAndRoleId(UUID authUserId, UUID roleId);
    
    boolean existsByAuthUserIdAndRoleId(UUID authUserId, UUID roleId);
}
