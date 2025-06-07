package org.example.service;

import com.auth0.jwt.JWT;
import org.springframework.beans.factory.annotation.Value;

import java.util.Date;

public class JWTUtil {

    @Value("${jwt_secret}")
    private String secret;

    @Value("${jwt_expiration}")
    private int expiration;

    public String generateToken(String username) {
        Date expirationDate = new Date(System.currentTimeMillis() + expiration);

        return "token";
    }
}
