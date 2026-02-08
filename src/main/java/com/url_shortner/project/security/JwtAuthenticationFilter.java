package com.url_shortner.project.security;

import com.url_shortner.project.service.JWTService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JWTService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long start = System.currentTimeMillis();

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            long duration = System.currentTimeMillis() - start;
            System.out.println("JwtAuthenticationFilter: No Bearer token found in header.");
            log.info("Middleware [JwtAuthenticationFilter] logic took {} ms (Skipped)", duration);
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        System.out.println(
                "JwtAuthenticationFilter: Token found: " + token.substring(0, Math.min(token.length(), 10)) + "...");
        try {
            Long userId = jwtService.getUserIdFromToken(token);
            System.out.println("JwtAuthenticationFilter: User ID extracted: " + userId);

            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Extract roles from token
                java.util.Set<String> roles = jwtService.getRolesFromToken(token);
                System.out.println("JwtAuthenticationFilter: Roles extracted: " + roles);
                java.util.List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(java.util.stream.Collectors.toList());

                // Fallback to ROLE_USER if no roles found
                if (authorities.isEmpty()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                }

                // Construct a principal object
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        authorities);

                SecurityContextHolder.getContext().setAuthentication(authToken);
                System.out.println("JwtAuthenticationFilter: Authentication set successfully.");
            }
        } catch (Exception e) {
            // Token validation failed
            System.out.println("JwtAuthenticationFilter: Token validation failed: " + e.getMessage());
            e.printStackTrace();
            log.error("Token validation failed: {}", e.getMessage());
        }

        long duration = System.currentTimeMillis() - start;
        log.info("Middleware [JwtAuthenticationFilter] logic took {} ms", duration);

        filterChain.doFilter(request, response);
    }
}
