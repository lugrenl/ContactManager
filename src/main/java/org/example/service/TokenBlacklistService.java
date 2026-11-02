package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dao.RevokedTokenDao;
import org.example.entity.RevokedToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RevokedTokenDao revokedTokenDao;
    private final JWTUtil jwtUtil;

    @Transactional
    public void blacklistToken(String token) {
        Instant expiresAt = jwtUtil.getExpiration(token);
        String tokenHash = fingerprint(token);

        revokedTokenDao.deleteExpired(Instant.now());
        if (!revokedTokenDao.existsByTokenHash(tokenHash)) {
            revokedTokenDao.save(new RevokedToken(tokenHash, expiresAt));
        }
    }

    @Transactional(readOnly = true)
    public boolean isTokenBlacklisted(String token) {
        return revokedTokenDao.existsByTokenHash(fingerprint(token));
    }

    static String fingerprint(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }
}
