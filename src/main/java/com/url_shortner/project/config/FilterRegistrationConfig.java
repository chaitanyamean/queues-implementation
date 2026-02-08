package com.url_shortner.project.config;

import com.url_shortner.project.interceptor.RateLimitInterceptor;
import com.url_shortner.project.security.JwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterRegistrationConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> registration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false); // Disable auto-registration in global chain
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RateLimitInterceptor> rateLimitRegistration(RateLimitInterceptor filter) {
        FilterRegistrationBean<RateLimitInterceptor> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false); // Disable auto-registration in global chain
        return registration;
    }
}
