package com.example.auth_service.application.service;

import com.example.auth_service.application.dto.AuthResponse;
import com.example.auth_service.application.dto.LoginRequest;
import com.example.auth_service.application.dto.RegisterRequest;
import com.example.auth_service.domain.event.UserRegisteredEvent;
import com.example.auth_service.domain.model.User;
import com.example.auth_service.infrastructure.event.UserEventProducer;
import com.example.auth_service.infrastructure.persistence.AuthUserRepository;
import com.example.auth_service.infrastructure.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserEventProducer userEventProducer;

    public AuthResponse register(RegisterRequest request) {
        if (authUserRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        authUserRepository.save(user);

        userEventProducer.sendUserRegisteredEvent(new UserRegisteredEvent(user.getEmail(), user.getCreatedAt()));

        String accessToken = jwtUtil.generateToken(user);

        AuthResponse response = AuthResponse.builder()
                .accessToken(accessToken)
                .build();
        return response;
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = authUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken = jwtUtil.generateToken(user);

        AuthResponse response = AuthResponse.builder()
                .accessToken(accessToken)
                .build();
        return response;
    }
}
