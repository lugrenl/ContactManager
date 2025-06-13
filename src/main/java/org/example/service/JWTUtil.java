package org.example.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Date;

@Component
public class JWTUtil {

    @Value("${jwt_secret}")
    private String secret;

    @Value("${jwt_expiration}")
    private int expiration;

    private static final String USER_NAME = "username";

    public String generateToken(String username) {
        Date expirationDate = Date.from(ZonedDateTime.now().plusMinutes(expiration).toInstant());

        return JWT.create()
                .withSubject("User details")
                .withExpiresAt(expirationDate)
                .withClaim(USER_NAME, username)
                .sign(Algorithm.HMAC256(secret));
    }

    public String validateTokenAndRetrieveClaim(String token) throws JWTVerificationException {
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret.getBytes())).build();

        DecodedJWT jwt = verifier.verify(token);

        if (jwt.getClaim(USER_NAME).isMissing()) {
            throw new JWTVerificationException("invalid token");
        }

        return jwt.getClaim(USER_NAME).asString();
    }
}
