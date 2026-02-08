package com.url_shortner.project.interceptor;

import com.url_shortner.project.service.RateLimitingService;
// import io.github.bucket4j.Bucket; // Removed
// import io.github.bucket4j.ConsumptionProbe; // Removed
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitInterceptor extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        String key = null;

        if (requestURI.equals("/shorten") && "POST".equalsIgnoreCase(method)) {
            org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                String role = authentication.getAuthorities().stream()
                        .findFirst()
                        .map(grantedAuthority -> grantedAuthority.getAuthority().replace("ROLE_", ""))
                        .orElse("HOBBY"); // Default to HOBBY if role missing (though unlikely with valid token)
                key = "user_" + role + "_" + authentication.getName();
            }
            // If authentication is null, we pass through (let Security handle it)
        } else if (requestURI.startsWith("/redirect") && "GET".equalsIgnoreCase(method)) {
            key = getClientIp(request);
        }

        if (key != null) {
            boolean allowed = rateLimitingService.isAllowed(key);
            long limit = rateLimitingService.getLimit(key);
            long remaining = rateLimitingService.getRemaining(key);
            // Reset is tricky with diverse strategies.
            // FixedWindow: End of window.
            // Sliding: Now + Window?
            // TokenBucket: When next token available?
            // For simplicity, we might omit Reset or provide a best guess.
            // Let's just set it to '0' or 'now' if not calculated, or better, remove it if
            // not needed.
            // But strict APIs usually want it. Let's send 0 for now as 'unknown' or 'N/A'
            // to avoid confusion.
            // Or better, if we know window is 1s, reset is next second.
            response.addHeader("X-RateLimit-Limit", String.valueOf(limit));
            response.addHeader("X-RateLimit-Remaining", String.valueOf(remaining));

            if (allowed) {
                // Allowed
                long duration = System.currentTimeMillis() - startTime;
                log.info("Middleware [RateLimitInterceptor] logic took {} ms (ALLOWED)", duration);
            } else {
                response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "You have exhausted your API Request Quota");

                long duration = System.currentTimeMillis() - startTime;
                log.info("Middleware [RateLimitInterceptor] logic took {} ms (BLOCKED)", duration);

                return; // Stop the chain
            }
        }

        // Proceed with the request
        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
