package com.saurabh.quickbill.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for JwtUtil — no Spring context needed, this class has no
 * Spring-managed collaborators other than the @Value-injected secret key,
 * which we set by hand with ReflectionTestUtils since @Value is only
 * processed when Spring builds the bean.
 */
class JwtUtilTest {

    // A throwaway 64-byte (128 hex char) key — HS512 requires a key of at
    // least 512 bits. This is NOT the real app secret; it exists only for
    // this test process and is never used to sign a real token.
    private static final String TEST_SECRET_HEX =
            "aa".repeat(64); // 128 hex chars = 64 bytes = 512 bits

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "SECRET_KEY", TEST_SECRET_HEX);
    }

    private UserDetails userDetails(String email) {
        // Password/authorities don't matter for these tests — JwtUtil only
        // reads the username via userDetails.getUsername().
        return new User(email, "irrelevant-password", java.util.List.of());
    }

    @Test
    void generateToken_thenExtractUsername_returnsTheOriginalEmail() {
        UserDetails user = userDetails("cashier@quickbill.com");

        String token = jwtUtil.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("cashier@quickbill.com");
    }

    @Test
    void validateToken_returnsTrue_whenTokenMatchesTheSameUser() {
        UserDetails user = userDetails("cashier@quickbill.com");
        String token = jwtUtil.generateToken(user);

        assertThat(jwtUtil.validateToken(token, user)).isTrue();
    }

    @Test
    void validateToken_returnsFalse_whenTokenBelongsToADifferentUser() {
        // Simulates: a token issued for one account being replayed against
        // a UserDetails lookup for a different account. Should never validate.
        UserDetails tokenOwner = userDetails("cashier@quickbill.com");
        UserDetails someoneElse = userDetails("admin@quickbill.com");

        String token = jwtUtil.generateToken(tokenOwner);

        assertThat(jwtUtil.validateToken(token, someoneElse)).isFalse();
    }

    @Test
    void extractExpiration_isInTheFuture_forAFreshlyIssuedToken() {
        UserDetails user = userDetails("cashier@quickbill.com");
        String token = jwtUtil.generateToken(user);

        assertThat(jwtUtil.extractExpiration(token))
                .isAfter(new java.util.Date());
    }
}
