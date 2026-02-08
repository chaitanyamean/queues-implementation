package com.url_shortner.project.service;

import com.url_shortner.project.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Service
public class JWTService {

    @Value("${jwt.secretKey}")
    private String jwtSecretKey;

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UserEntity user) {
        return Jwts.builder().setSubject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", user.getRoles().stream()
                        .map(Enum::name)
                        .collect(java.util.stream.Collectors.toSet()))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 25)) // 25 hours expiry
                .signWith(getSecretKey())
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = getClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    public java.util.Set<String> getRolesFromToken(String token) {
        Claims claims = getClaims(token);
        List<?> roles = claims.get("roles", List.class);
        if (roles == null) {
            return java.util.Collections.emptySet();
        }
        return roles.stream()
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toSet());
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
