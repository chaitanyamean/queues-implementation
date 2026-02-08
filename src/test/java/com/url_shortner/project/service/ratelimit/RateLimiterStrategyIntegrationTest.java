package com.url_shortner.project.service.ratelimit;

import com.url_shortner.project.config.RedisConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@org.springframework.test.context.TestPropertySource(properties = {
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "spring.data.redis.password="
})
public class RateLimiterStrategyIntegrationTest {

    @Autowired
    private FixedWindowStrategy fixedWindowStrategy;

    @Autowired
    private SlidingWindowStrategy slidingWindowStrategy;

    @Autowired
    private TokenBucketStrategy tokenBucketStrategy;

    @Autowired
    private LeakyBucketStrategy leakyBucketStrategy;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private String userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
    }

    @Test
    void testFixedWindowStrategy() {
        String key = "fixed_" + userId;
        // Limit 5 req / 2 sec
        boolean allowed;
        for (int i = 0; i < 5; i++) {
            allowed = fixedWindowStrategy.isAllowed(key, 5, 2);
            assertTrue(allowed, "Request " + i + " should be allowed");
        }
        allowed = fixedWindowStrategy.isAllowed(key, 5, 2);
        assertFalse(allowed, "Request 6 should be blocked");
    }

    @Test
    void testSlidingWindowStrategy() throws InterruptedException {
        String key = "sliding_" + userId;
        // Limit 3 req / 1 sec
        assertTrue(slidingWindowStrategy.isAllowed(key, 3, 1));
        assertTrue(slidingWindowStrategy.isAllowed(key, 3, 1));
        assertTrue(slidingWindowStrategy.isAllowed(key, 3, 1));
        assertFalse(slidingWindowStrategy.isAllowed(key, 3, 1));

        // Wait for window to slide (1.1 sec)
        Thread.sleep(1100);
        assertTrue(slidingWindowStrategy.isAllowed(key, 3, 1));
    }

    @Test
    void testTokenBucketStrategy() throws InterruptedException {
        String key = "token_" + userId;
        // Limit 10 (Capacity), Rate = 10/1s = 10 tokens/sec
        // We can burst 10.
        for (int i = 0; i < 10; i++) {
            assertTrue(tokenBucketStrategy.isAllowed(key, 10, 1), "Token " + i);
        }
        assertFalse(tokenBucketStrategy.isAllowed(key, 10, 1));

        // Wait 100ms -> refill 1 token?
        Thread.sleep(150);
        assertTrue(tokenBucketStrategy.isAllowed(key, 10, 1));
    }

    @Test
    void testLeakyBucketStrategy() throws InterruptedException {
        String key = "leaky_" + userId;
        // Limit 5 req / 1 sec => Interval = 200ms

        // 1st request: Allowed (sets next allowed to now + 200ms)
        assertTrue(leakyBucketStrategy.isAllowed(key, 5, 1));

        // Immediate 2nd request: Blocked (too soon)
        assertFalse(leakyBucketStrategy.isAllowed(key, 5, 1));

        // Wait > 200ms
        Thread.sleep(250);
        assertTrue(leakyBucketStrategy.isAllowed(key, 5, 1));
    }
}
