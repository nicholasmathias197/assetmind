package com.assetmind.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JwtTokenProvider {

    private static final int HS512_MIN_KEY_BYTES = 64;

    private final SecretKey key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret:assetmind-local-jwt-secret-for-hs512-development-only-change-this-before-production-2026}") String secret,
            @Value("${jwt.access-token-expiration:3600000}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration:86400000}") long refreshTokenExpiration) {
        this.key = createSigningKey(secret);
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String generateAccessToken(String userId, String username, String role, List<String> featureAccess) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("role", role);
        claims.put("featureAccess", featureAccess);
        return createToken(claims, userId, accessTokenExpiration);
    }

    public String generateRefreshToken(String userId) {
        return createToken(new HashMap<>(), userId, refreshTokenExpiration);
    }

    private String createToken(Map<String, Object> claims, String subject, long expirationTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .addClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    public String getUserIdFromToken(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return (String) extractAllClaims(token).get("role");
    }

    @SuppressWarnings("unchecked")
    public List<String> getFeatureAccessFromToken(String token) {
        Object featureAccess = extractAllClaims(token).get("featureAccess");
        if (featureAccess instanceof List<?>) {
            return ((List<?>) featureAccess).stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }
        return List.of();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    private SecretKey createSigningKey(String secret) {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalArgumentException("jwt.secret must not be blank");
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < HS512_MIN_KEY_BYTES) {
            keyBytes = sha512(keyBytes);
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    private byte[] sha512(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-512").digest(value);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-512 algorithm is not available", e);
        }
    }
}

