package com.example.auth_service.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 configuration for Auth Service.
 *
 * <p>Swagger UI:  /swagger-ui.html</p>
 * <p>API docs:    /v3/api-docs</p>
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:auth-service}")
    private String applicationName;

    @Bean
    public OpenAPI authServiceOpenAPI() {
        final String schemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Auth Service API")
                        .version("1.0.0")
                        .description("""
                                Authentication & authorization microservice.
                                Issues JWT tokens (HS384), manages users, roles, and password resets.
                                Consumed by the API Gateway for the `/api/auth/validate` check on every protected request.
                                """)
                        .contact(new Contact().name("Platform Team").email("platform@example.com"))
                        .license(new License().name("Internal")))
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("Local direct"),
                        new Server().url("http://localhost:8080").description("Via API Gateway"),
                        new Server().url("http://ticketing.local").description("K8s ingress")))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components()
                        .addSecuritySchemes(schemeName, new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the `accessToken` returned by POST /api/auth/login")));
    }
}
