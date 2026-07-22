package com.example.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final Duration expiration;

    public JwtTokenProvider(JwtProperties properties) {
        try {
            this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SECRET must be a Base64-encoded key of at least 32 bytes.", exception);
        }
        this.expiration = Duration.ofMinutes(properties.expirationMinutes());
    }

    public String createToken(Long employeeId, String employeeNo) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(employeeNo)
                .claim("employeeId", employeeId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }
}
