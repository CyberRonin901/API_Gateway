package com.cyberronin.authservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationTime;

    public JwtUtil(@Value("${JWT_SECRET}") String secret,
                   @Value("${JWT_EXPIRATION:3600000}") long expirationTime)
    {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationTime = expirationTime;
    }

    // generate jwt token with username as Subject and custom claims
    public String generateToken(String username, Long userId, String role) {
        return Jwts.builder()
                .subject(userId.toString()) // sub : username
                .claims(Map.of("username", username, "role", role))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).get("username", String.class);
    }

    public String extractRole(String token){
        return extractAllClaims(token).get("role", String.class);
    }

    public String extractUserId(String token){
        return extractAllClaims(token).getSubject();
    }

    // extract all the claims from the JWT
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Unused method for validating JWT, validation is done at the API gateway and contains the same method
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }
}