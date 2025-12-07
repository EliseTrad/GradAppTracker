package com.gradapptracker.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;

/**
 * Utility component for JWT token generation, parsing, and validation.
 * <p>
 * Handles all JWT operations including:
 * <ul>
 * <li>Token generation with user ID and username claims</li>
 * <li>Token validation and signature verification</li>
 * <li>Extracting user information from tokens</li>
 * </ul>
 * <p>
 * Uses HMAC-SHA256 signing algorithm with a secret key configured in
 * application.properties. Tokens expire after a configurable duration.
 */
@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expirationMs}")
    private long jwtExpirationMs;

    private Key signingKey;

    @PostConstruct
    private void init() {
        // create signing key from secret
        signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(UserPrincipal principal) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(principal.getUsername())
                .claim("userId", principal.getId())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Integer getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Object userId = claims.get("userId");
            if (userId instanceof Integer) {
                return (Integer) userId;
            }
            // jjwt may deserialize numbers as Integer or Long depending on value
            if (userId instanceof Number) {
                return ((Number) userId).intValue();
            }
            return null;
        } catch (JwtException | IllegalArgumentException ex) {
            logger.warn("Failed to parse JWT token to extract userId: {}", ex.getMessage());
            return null;
        }
    }

    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException ex) {
            logger.warn("Failed to parse JWT token to extract username: {}", ex.getMessage());
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException ex) {
            logger.warn("Invalid JWT signature: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.warn("JWT token is expired: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.warn("JWT token is unsupported: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.warn("JWT claims string is empty: {}", ex.getMessage());
        } catch (JwtException ex) {
            logger.warn("JWT validation failed: {}", ex.getMessage());
        }

        return false;
    }
}
