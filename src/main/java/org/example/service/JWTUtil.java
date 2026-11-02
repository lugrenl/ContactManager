package org.example.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Component
public class JWTUtil {

    static final int MIN_SECRET_LENGTH_BYTES = 32;
    private static final String INSECURE_DEFAULT_SECRET = "SECRET";
    private static final String USER_NAME = "username";

    private final String secret;
    private final long expirationMinutes;

    public JWTUtil(
            @Value("${jwt_secret:}") String secret,
            @Value("${jwt_expiration:0}") long expirationMinutes) {
        this.secret = validateSecret(secret);
        this.expirationMinutes = validateExpiration(expirationMinutes);
    }

    public String generateToken(String username) {
        Instant now = Instant.now();
        Date expirationDate = Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES));

        return JWT.create()
                .withSubject("User details")
                .withIssuedAt(Date.from(now))
                .withJWTId(UUID.randomUUID().toString())
                .withExpiresAt(expirationDate)
                .withClaim(USER_NAME, username)
                .sign(Algorithm.HMAC256(secret));
    }

    public String validateTokenAndRetrieveClaim(String token) throws JWTVerificationException {
        DecodedJWT jwt = createVerifier().verify(token);

        if (jwt.getClaim(USER_NAME).isMissing()) {
            throw new JWTVerificationException("invalid token");
        }

        return jwt.getClaim(USER_NAME).asString();
    }

    public Instant getExpiration(String token) throws JWTVerificationException {
        DecodedJWT jwt = createVerifier().verify(token);
        Date expiresAt = jwt.getExpiresAt();
        if (expiresAt == null) {
            throw new JWTVerificationException("token has no expiration");
        }
        return expiresAt.toInstant();
    }

    private JWTVerifier createVerifier() {
        return JWT.require(Algorithm.HMAC256(secret)).build();
    }

    private static String validateSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT signing secret is not configured. Set the JWT_SECRET environment variable "
                            + "(or the jwt_secret property) to a value of at least "
                            + MIN_SECRET_LENGTH_BYTES + " characters.");
        }
        String normalized = secret.trim();
        if (INSECURE_DEFAULT_SECRET.equals(normalized)) {
            throw new IllegalStateException(
                    "JWT signing secret uses a known insecure default value. Configure a unique "
                            + "JWT_SECRET environment variable instead.");
        }
        if (normalized.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_LENGTH_BYTES) {
            throw new IllegalStateException(
                    "JWT signing secret is too short. It must be at least "
                            + MIN_SECRET_LENGTH_BYTES + " characters.");
        }
        return normalized;
    }

    private static long validateExpiration(long expirationMinutes) {
        if (expirationMinutes <= 0) {
            throw new IllegalStateException(
                    "JWT expiration is not configured correctly. Set jwt_expiration "
                            + "to a positive number of minutes.");
        }
        return expirationMinutes;
    }
}
