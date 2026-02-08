package com.url_shortner.project.service.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component("slidingWindowStrategy")
@RequiredArgsConstructor
public class SlidingWindowStrategy implements RateLimiterStrategy {

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean isAllowed(String key, long limit, long windowSeconds) {
        String redisKey = "rate_limit:sliding:" + key;
        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - (windowSeconds * 1000);

        try {
            ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();

            // 1. Remove old requests outside the window
            zSetOps.removeRangeByScore(redisKey, 0, windowStart);

            // 2. Count current requests
            Long count = zSetOps.zCard(redisKey);

            if (count != null && count < limit) {
                // 3. Add current request
                zSetOps.add(redisKey, UUID.randomUUID().toString(), currentTime);
                // 4. Set expiry to clean up if inactive
                redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
                return true;
            }

            return false;
        } catch (Exception e) {
            return true; // Fail open
        }
    }

    @Override
    public long getRemaining(String key, long limit, long windowSeconds) {
        String redisKey = "rate_limit:sliding:" + key;
        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - (windowSeconds * 1000);

        try {
            ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
            zSetOps.removeRangeByScore(redisKey, 0, windowStart);
            Long count = zSetOps.zCard(redisKey);
            return count != null ? Math.max(0, limit - count) : limit;
        } catch (Exception e) {
            return 0;
        }
    }
}
