package com.url_shortner.project.service;

import com.url_shortner.project.service.ratelimit.RateLimiterStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class RateLimitingService {

    private final Map<String, RateLimiterStrategy> strategies;

    @Value("${app.rate-limiting.strategy:tokenBucketStrategy}")
    private String activeStrategyName;

    /**
     * Checks if the request is allowed based on the active strategy.
     */
    public boolean isAllowed(String key) {
        RateLimitRule rule = getRule(key);
        RateLimiterStrategy strategy = strategies.get(activeStrategyName);
        if (strategy == null) {
            log.warn("Strategy {} not found, defaulting to fixedWindowStrategy", activeStrategyName);
            strategy = strategies.get("fixedWindowStrategy");
        }
        return strategy.isAllowed(key, rule.limit, rule.windowSeconds);
    }

    public long getLimit(String key) {
        return getRule(key).limit;
    }

    public long getRemaining(String key) {
        RateLimitRule rule = getRule(key);
        RateLimiterStrategy strategy = strategies.get(activeStrategyName);
        if (strategy == null)
            strategy = strategies.get("fixedWindowStrategy");
        return strategy.getRemaining(key, rule.limit, rule.windowSeconds);
    }

    private RateLimitRule getRule(String key) {
        if (key.startsWith("user_")) {
            if (key.contains("_FREE_")) {
                // Free plan: 5 requests per 60 seconds
                return new RateLimitRule(5, 60);
            }
            // HOBBY/ENTERPRISE: 10 requests per second
            return new RateLimitRule(10, 1);
        } else {
            // Public redirect: 50 requests per second
            return new RateLimitRule(50, 1);
        }
    }

    private record RateLimitRule(long limit, long windowSeconds) {
    }
}
