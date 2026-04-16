package com.example.auth_service.application.port;

import com.example.auth_service.application.dto.*;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    void logout(String refreshToken);
    AuthResponse refreshToken(String refreshToken);
    TokenValidationResponse validateToken(String accessToken);
    void changePassword(ChangePasswordRequest request);
    void resetPassword(String email);
}
