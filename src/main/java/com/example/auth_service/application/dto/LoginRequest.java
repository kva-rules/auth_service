package com.example.auth_service.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Credentials for POST /api/auth/login. Returns a JWT access token + refresh token.")
public class LoginRequest {

    @Email
    @NotBlank
    @Schema(description = "User email (unique identifier)", example = "alice@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank
    @Schema(description = "Plain-text password — bcrypt-compared server-side", example = "Passw0rd!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
