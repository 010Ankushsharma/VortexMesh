package com.vortexmesh.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private static final String SECRET = "VortexMesh-Secret-Key-Must-Be-At-Least-256-Bits-Long!!";
    private static final long EXPIRATION_MS = 3600000; // 1 hour
    private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    public String generateToken(String subject, Map<String, Object> claims) {
        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusMillis(EXPIRATION_MS)))
                .signWith(key)
                .compact();
    }

    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            validateToken(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public String extractSubject(String token) {
        return validateToken(token).getSubject();
    }
}
