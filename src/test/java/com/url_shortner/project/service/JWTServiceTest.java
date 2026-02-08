package com.url_shortner.project.service;

import com.url_shortner.project.entity.Role;
import com.url_shortner.project.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JWTServiceTest {

    @InjectMocks
    private JWTService jwtService;

    private final String secretKey = "mySecretKeyMySecretKeyMySecretKeyMySecretKey"; // Must be >= 256 bits

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(jwtService, "jwtSecretKey", secretKey);
    }

    @Test
    void testGenerateToken_And_GetUserIdFromToken() {
        // Arrange
        UserEntity user = new UserEntity();
        user.setId(123L);
        user.setEmail("test@example.com");
        Set<Role> roles = new HashSet<>();
        roles.add(Role.HOBBY);
        roles.add(Role.ENTERPRISE);
        user.setRoles(roles);

        // Act
        String token = jwtService.generateToken(user);
        System.out.println("Generated Token: " + token);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Validate Token Parsing
        Long userId = jwtService.getUserIdFromToken(token);
        assertEquals(123L, userId);

        // Validate Roles Claim
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                .build()
                .parseClaimsJws(token)
                .getBody();

        List<String> tokenRoles = (List<String>) claims.get("roles");
        assertNotNull(tokenRoles);
        assertTrue(tokenRoles.contains("USER"));
        assertTrue(tokenRoles.contains("ADMIN"));
    }
}
