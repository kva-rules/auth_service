package com.example.auth_service.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload for POST /api/auth/register. Creates a new user and returns a JWT.")
public class RegisterRequest {

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    @Schema(description = "Unique user email", example = "alice@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d).+$",
            message = "Password must contain at least 1 uppercase letter and 1 number"
    )
    @Schema(description = "Must be ≥8 chars with at least 1 uppercase + 1 digit", example = "Passw0rd!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "Role to assign — one of USER | ENGINEER | MANAGER | ADMIN. Defaults to USER if omitted.", example = "USER")
    private String role;
}
