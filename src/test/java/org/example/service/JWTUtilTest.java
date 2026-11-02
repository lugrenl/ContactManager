package org.example.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JWTUtilTest {

    private static final String SECRET = "unit-test-secret-0123456789abcdef-0123456789";
    private static final String OTHER_SECRET = "other-unit-test-secret-0123456789abcdef-01";

    private final JWTUtil jwtUtil = new JWTUtil(SECRET, 60);

    @Test
    void validTokenIsAccepted() {
        String token = jwtUtil.generateToken("testuser");

        assertThat(jwtUtil.validateTokenAndRetrieveClaim(token)).isEqualTo("testuser");
    }

    @Test
    void expiredTokenIsRejected() {
        String token = signedToken("testuser", Instant.now().minusSeconds(60));

        assertThatThrownBy(() -> jwtUtil.validateTokenAndRetrieveClaim(token))
                .isInstanceOf(TokenExpiredException.class);
    }

    @Test
    void tokenNearingExpirationIsStillAccepted() {
        String token = signedToken("testuser", Instant.now().plusSeconds(3));

        assertThat(jwtUtil.validateTokenAndRetrieveClaim(token)).isEqualTo("testuser");
    }

    @Test
    void tokenWithWrongSignatureIsRejected() {
        String token = JWT.create()
                .withClaim("username", "testuser")
                .withExpiresAt(Date.from(Instant.now().plusSeconds(60)))
                .sign(Algorithm.HMAC256(OTHER_SECRET));

        assertThatThrownBy(() -> jwtUtil.validateTokenAndRetrieveClaim(token))
                .isInstanceOf(JWTVerificationException.class);
    }

    @Test
    void malformedTokenIsRejected() {
        assertThatThrownBy(() -> jwtUtil.validateTokenAndRetrieveClaim("not-a-jwt"))
                .isInstanceOf(JWTVerificationException.class);
    }

    @Test
    void tokenWithoutUsernameIsRejected() {
        String token = JWT.create()
                .withExpiresAt(Date.from(Instant.now().plusSeconds(60)))
                .sign(Algorithm.HMAC256(SECRET));

        assertThatThrownBy(() -> jwtUtil.validateTokenAndRetrieveClaim(token))
                .isInstanceOf(JWTVerificationException.class);
    }

    @Test
    void generatedTokenExpiresAfterConfiguredMinutes() {
        String token = jwtUtil.generateToken("testuser");

        Instant expiresAt = jwtUtil.getExpiration(token);
        long minutes = Duration.between(Instant.now(), expiresAt).toMinutes();

        assertThat(minutes).isBetween(58L, 60L);
    }

    @Test
    void configuredExpirationIsInterpretedAsMinutes() {
        JWTUtil tenMinutes = new JWTUtil(SECRET, 10);
        String token = tenMinutes.generateToken("testuser");

        long minutes = Duration.between(Instant.now(), tenMinutes.getExpiration(token)).toMinutes();

        assertThat(minutes).isBetween(8L, 10L);
    }

    @Test
    void rejectsMissingSecret() {
        assertThatThrownBy(() -> new JWTUtil("", 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void rejectsKnownDefaultSecret() {
        assertThatThrownBy(() -> new JWTUtil("SECRET", 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("insecure default");
    }

    @Test
    void rejectsShortSecret() {
        assertThatThrownBy(() -> new JWTUtil("too-short-secret", 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("too short");
    }

    @Test
    void rejectsNonPositiveExpiration() {
        assertThatThrownBy(() -> new JWTUtil(SECRET, 0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt_expiration");
    }

    private static String signedToken(String username, Instant expiresAt) {
        return JWT.create()
                .withClaim("username", username)
                .withExpiresAt(Date.from(expiresAt))
                .sign(Algorithm.HMAC256(SECRET));
    }
}
