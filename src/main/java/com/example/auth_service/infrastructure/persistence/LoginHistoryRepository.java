package com.example.auth_service.infrastructure.persistence;

import com.example.auth_service.domain.model.LoginHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, UUID> {
    List<LoginHistory> findByAuthUserIdOrderByLoginTimeDesc(UUID authUserId);
    
    Page<LoginHistory> findByAuthUserIdOrderByLoginTimeDesc(UUID authUserId, Pageable pageable);
}
