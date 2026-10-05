package com.example.autotest_backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtUtil}.
 *
 * <p>No mocks here: the class under test is pure cryptography, so the tests sign
 * real tokens and read them back. The negative cases matter most, because a token
 * that stays valid after expiry or accepts a foreign signature is a security hole.
 */
@DisplayName("JwtUtil")
class JwtUtilTest {

    private static final String SECRET = "test_secret_key_that_is_long_enough_for_hs256";
    private static final String OTHER_SECRET = "another_secret_key_long_enough_for_hs256_too";
    private static final long ONE_HOUR = 3600L;

    @Test
    @DisplayName("signs a token that can be read back with subject and role")
    void signsAndValidatesToken() {
        // Arrange
        JwtUtil jwtUtil = new JwtUtil(SECRET, ONE_HOUR);

        // Act
        String token = jwtUtil.generateToken("teacher@example.com", "TEACHER");
        Claims claims = jwtUtil.validateToken(token);

        // Assert
        assertThat(claims.getSubject()).isEqualTo("teacher@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("TEACHER");
    }

    @Test
    @DisplayName("sets the expiry the configured number of seconds ahead")
    void setsConfiguredExpiry() {
        // Arrange
        JwtUtil jwtUtil = new JwtUtil(SECRET, ONE_HOUR);
        long before = System.currentTimeMillis();

        // Act
        Claims claims = jwtUtil.validateToken(jwtUtil.generateToken("user@example.com", "STUDENT"));

        // Assert: expiry lands one hour ahead, allowing a generous clock margin
        Date expiration = claims.getExpiration();
        assertThat(expiration).isAfter(new Date(before));
        assertThat(expiration.getTime() - before)
                .isBetween(ONE_HOUR * 1000 - 5_000, ONE_HOUR * 1000 + 5_000);
    }

    @Test
    @DisplayName("reports the configured lifetime through getExpirationSeconds")
    void reportsExpirationSeconds() {
        // Arrange + Act + Assert
        assertThat(new JwtUtil(SECRET, ONE_HOUR).getExpirationSeconds()).isEqualTo(ONE_HOUR);
    }

    @Test
    @DisplayName("rejects an expired token")
    void rejectsExpiredToken() {
        // Arrange: a negative lifetime produces a token that expired before it was issued
        JwtUtil expiringUtil = new JwtUtil(SECRET, -60L);
        String expiredToken = expiringUtil.generateToken("user@example.com", "STUDENT");

        // Act + Assert
        assertThatThrownBy(() -> expiringUtil.validateToken(expiredToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("rejects a token signed with a different secret")
    void rejectsForeignSignature() {
        // Arrange
        String foreignToken = new JwtUtil(OTHER_SECRET, ONE_HOUR)
                .generateToken("attacker@example.com", "TEACHER");
        JwtUtil jwtUtil = new JwtUtil(SECRET, ONE_HOUR);

        // Act + Assert
        assertThatThrownBy(() -> jwtUtil.validateToken(foreignToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("rejects a malformed token")
    void rejectsMalformedToken() {
        // Arrange
        JwtUtil jwtUtil = new JwtUtil(SECRET, ONE_HOUR);

        // Act + Assert
        assertThatThrownBy(() -> jwtUtil.validateToken("not-a-jwt"))
                .isInstanceOf(JwtException.class);
    }
}
