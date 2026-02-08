package com.url_shortner.project.service.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component("fixedWindowStrategy")
@RequiredArgsConstructor
public class FixedWindowStrategy implements RateLimiterStrategy {

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean isAllowed(String key, long limit, long windowSeconds) {
        String redisKey = "rate_limit:fixed:" + key;

        try {
            Long count = redisTemplate.opsForValue().increment(redisKey);

            if (count != null && count == 1) {
                redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
            }

            return count != null && count <= limit;
        } catch (Exception e) {
            // Fail open if Redis is down
            return true;
        }
    }

    @Override
    public long getRemaining(String key, long limit, long windowSeconds) {
        String redisKey = "rate_limit:fixed:" + key;
        String val = redisTemplate.opsForValue().get(redisKey);
        if (val == null) {
            return limit;
        }
        return Math.max(0, limit - Long.parseLong(val));
    }
}
