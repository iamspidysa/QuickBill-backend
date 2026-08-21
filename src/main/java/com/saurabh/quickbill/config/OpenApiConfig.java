package com.saurabh.quickbill.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the generated OpenAPI/Swagger document.
 *
 * Two things this does that you don't get for free just by adding the
 * springdoc dependency:
 *
 *  1. @SecurityScheme registers a "bearerAuth" scheme (JWT in the
 *     Authorization header). Without this, Swagger UI has no "Authorize"
 *     button, and every protected endpoint (almost all of them — see
 *     SecurityConfig) would show 401 when you try it from the docs page,
 *     because no token is ever sent.
 *
 *  2. @SecurityRequirement applies that scheme to every endpoint by
 *     default, so you don't have to annotate each controller individually.
 *     Click "Authorize" once in the UI, paste a token, and it's attached
 *     to every request you try from then on.
 *
 * No @Bean method is needed here — springdoc picks up these annotations
 * automatically at startup because this class is on the component-scanned
 * classpath (any class works; a small dedicated config class is just the
 * conventional place to put them).
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "QuickBill API",
                version = "v1",
                description = "REST API for the QuickBill billing / point-of-sale system."
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Paste the JWT returned by POST /login here (without the word 'Bearer')."
)
public class OpenApiConfig {
}
