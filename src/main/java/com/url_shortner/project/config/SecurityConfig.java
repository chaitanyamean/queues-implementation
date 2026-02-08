package com.url_shortner.project.config;

import com.url_shortner.project.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final com.url_shortner.project.interceptor.RateLimitInterceptor rateLimitInterceptor;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/redirect/**", "/health", "/error", "/ws/**").permitAll() // Allow
                        // authentication,
                        // redirect, health, and error
                        // endpoints
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/users").permitAll() // Allow user
                                                                                                         // registration
                                                                                                         // ONLY
                                                                                                         // registration
                                                                                                         // ONLY
                        .anyRequest().authenticated() // Protect everything else
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitInterceptor, JwtAuthenticationFilter.class);

        return http.build();
    }
}
