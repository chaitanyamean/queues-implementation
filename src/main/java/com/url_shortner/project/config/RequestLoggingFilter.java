package com.url_shortner.project.config;

import com.url_shortner.project.entity.RequestLogEntity;
import com.url_shortner.project.repository.RequestLogRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final RequestLogRepository requestLogRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();

        // 1. Capture Request Details
        String method = request.getMethod();
        String url = request.getRequestURI();
        String userAgent = request.getHeader("User-Agent");
        String ipAddress = getClientIp(request); // Helper method for correct IP

        // 2. Log to Console (for debugging)
        log.info("Incoming Request: {} {} from IP: {}", method, url, ipAddress);

        // 3. Save to Database
        // Note: In high-traffic apps, we'd wrap this in an async block or use a queue
        // so we don't slow down the user's response time.
        RequestLogEntity logEntity = RequestLogEntity.builder()
                .httpMethod(method)
                .url(url)
                .clientIp(ipAddress)
                .userAgent(userAgent)
                .timestamp(LocalDateTime.now())
                .build();

        try {
            requestLogRepository.save(logEntity);
        } catch (Exception e) {
            log.error("Failed to save request log", e);
        }

        // 4. Log Middleware Duration
        long duration = System.currentTimeMillis() - start;
        log.info("Middleware [RequestLoggingFilter] logic took {} ms", duration);

        // 5. Continue the chain (Let the request go to the Controller)
        filterChain.doFilter(request, response);
    }

    // Helper to handle Load Balancers / Proxies
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For can be a list "10.0.0.1, 192.168.1.1", we want the first one
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0];
        }
        return ip;
    }

    // @Override
    // protected boolean shouldNotFilter(HttpServletRequest request) throws
    // ServletException {
    // String path = request.getRequestURI();
    //
    // // LOGIC: Skip this filter if the path is "/auth/login" or "/health"
    // // You can also use startsWith() for groups of routes
    // return path.startsWith("/redirect");
    // }
}