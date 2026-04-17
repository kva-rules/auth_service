package com.example.auth_service.infrastructure.security;

import com.example.auth_service.domain.model.User;
import com.example.auth_service.infrastructure.persistence.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service("userSecurityService")
@RequiredArgsConstructor
public class UserSecurityService {

    private final AuthUserRepository authUserRepository;

    public boolean isCurrentUser(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User currentUser) {
            if (currentUser.getUserId() != null && currentUser.getUserId().equals(userId)) {
                return true;
            }
            // Also check by looking up the user
            Optional<User> targetUser = authUserRepository.findByUserId(userId);
            return targetUser.map(user -> user.getAuthUserId().equals(currentUser.getAuthUserId())).orElse(false);
        }
        return false;
    }
}
