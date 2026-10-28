package com.codelens.user.service;

import com.codelens.user.config.SecurityProperties;
import com.codelens.user.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Firma los tokens HS256. El gateway los valida con el mismo secreto, asi que
 * este servicio no necesita exponer un endpoint de validacion.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final SecurityProperties properties;

    public JwtService(SecurityProperties properties) {
        byte[] secret = properties.getJwtSecret() == null ? new byte[0]
                : properties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("codelens.security.jwt-secret debe tener al menos 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.properties = properties;
    }

    public IssuedToken issue(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.getTokenTtl());
        String token = Jwts.builder()
                .subject(user.getId())
                .claim("username", user.getUsername())
                .claim("name", user.getFullName())
                .issuer("codelens-user-service")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }
}
