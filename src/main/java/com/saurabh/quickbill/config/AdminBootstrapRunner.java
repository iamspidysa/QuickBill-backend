package com.saurabh.quickbill.config;

import com.saurabh.quickbill.entity.UserEntity;
import com.saurabh.quickbill.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Creates the very first ROLE_ADMIN account on application startup.
 *
 * Why this exists: POST /admin/register requires an already-authenticated
 * ROLE_ADMIN caller (see SecurityConfig). Without a bootstrap step, there is
 * no way to create the first admin account through the API at all — the only
 * option is inserting a row directly into the database, which is a manual,
 * error-prone, undocumented step. This runner removes that gap.
 *
 * Behaviour:
 *  - Runs on every startup, but only ever creates a user the first time no
 *    ROLE_ADMIN account exists yet. Once an admin exists, this is a no-op —
 *    safe to leave enabled permanently.
 *  - Reads credentials from environment variables (ADMIN_EMAIL /
 *    ADMIN_PASSWORD / ADMIN_NAME) rather than hardcoding them, so no
 *    credentials live in source control.
 *  - If those env vars are not set, it logs a clear warning and skips —
 *    it never invents a default password.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.email:}")
    private String adminEmail;

    @Value("${app.bootstrap-admin.password:}")
    private String adminPassword;

    @Value("${app.bootstrap-admin.name:Admin}")
    private String adminName;

    @Override
    public void run(String... args) {
        if (userRepository.existsByRole("ROLE_ADMIN")) {
            log.debug("Admin bootstrap: a ROLE_ADMIN user already exists — skipping.");
            return;
        }

        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.warn("Admin bootstrap: no ROLE_ADMIN account exists yet, and ADMIN_EMAIL / " +
                    "ADMIN_PASSWORD are not set, so none can be created automatically. " +
                    "Set both environment variables and restart the app to create the first admin.");
            return;
        }

        if (userRepository.existsByEmail(adminEmail)) {
            log.warn("Admin bootstrap: ADMIN_EMAIL '{}' is already registered to a non-admin " +
                    "user — skipping automatic creation to avoid overwriting an existing account. " +
                    "Promote that user manually, or set ADMIN_EMAIL to a different address.", adminEmail);
            return;
        }

        UserEntity admin = UserEntity.builder()
                .userId(UUID.randomUUID().toString())
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .role("ROLE_ADMIN")
                .name(adminName)
                .build();

        userRepository.save(admin);
        log.info("Admin bootstrap: created the first ROLE_ADMIN account for '{}'. " +
                "Log in with this account and use /admin/register to create further users.", adminEmail);
    }
}
