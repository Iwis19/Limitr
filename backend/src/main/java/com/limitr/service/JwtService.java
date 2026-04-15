package com.limitr.service;

import com.limitr.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMinutes;

    public JwtService(JwtProperties jwtProperties) {
        validate(jwtProperties);
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = jwtProperties.getExpirationMinutes();
    }

    public String generateToken(String username) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(username)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(expirationMinutes * 60)))
            .signWith(signingKey)
            .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, String username) {
        try {
            Claims claims = parseClaims(token);
            return username.equals(claims.getSubject()) && claims.getExpiration().after(new Date());
        } catch (Exception exception) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private void validate(JwtProperties jwtProperties) {
        String secret = jwtProperties.getSecret();
        int secretLength = secret.getBytes(StandardCharsets.UTF_8).length;

        if (secret.isBlank()) {
            throw new IllegalStateException("app.jwt.secret must be configured.");
        }

        if (secretLength < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes long.");
        }

        if (!jwtProperties.isAllowInsecureSecret()
            && JwtProperties.LOCAL_DEVELOPMENT_SECRET.equals(secret)) {
            throw new IllegalStateException("Configure a unique app.jwt.secret for this environment.");
        }
    }
}
