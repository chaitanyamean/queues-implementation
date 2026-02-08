package com.url_shortner.project.service.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Component("leakyBucketStrategy")
@RequiredArgsConstructor
public class LeakyBucketStrategy implements RateLimiterStrategy {

    private final StringRedisTemplate redisTemplate;

    // Lua script for Leaky Bucket (Strict Spacing / GCRA with burst=0)
    // Ensures requests are spaced out by at least (1 / rate) seconds.
    private final String luaScript = "local key = KEYS[1] " +
            "local interval_micros = tonumber(ARGV[1]) " + // Gap required between requests
            "local now_micros = tonumber(ARGV[2]) " +

            "local next_allowed_key = key .. ':next_allowed' " +
            "local next_allowed = tonumber(redis.call('get', next_allowed_key)) " +

            "if next_allowed == nil then\n" +
            "   next_allowed = now_micros\n" +
            "else\n" +
            "   if now_micros < next_allowed then\n" +
            "       return -1\n" +
            "   end\n" +
            "end\n" +

            "local new_next_allowed = now_micros + interval_micros\n" +
            "redis.call('set', next_allowed_key, new_next_allowed)\n" +
            "redis.call('expire', next_allowed_key, 60)\n" +

            "return 1";

    private final RedisScript<Long> script = new DefaultRedisScript<>(luaScript, Long.class);

    @Override
    public boolean isAllowed(String key, long limit, long windowSeconds) {
        String redisKey = "rate_limit:leaky_bucket:" + key;
        long nowMicros = System.currentTimeMillis() * 1000;

        // Interval in microseconds = (Window in Seconds * 1,000,000) / Limit
        // Example: 10 req / 1 sec => 100,000 micros interval (100ms)
        long intervalMicros = (windowSeconds * 1_000_000) / limit;

        List<String> keys = Collections.singletonList(redisKey);
        Long result = redisTemplate.execute(script, keys, String.valueOf(intervalMicros), String.valueOf(nowMicros));

        return result != null && result == 1;
    }

    @Override
    public long getRemaining(String key, long limit, long windowSeconds) {
        // Leaky bucket is binary (pass/fail based on spacing), remaining capacity is
        // hard to define in tokens.
        // We can just return 1 or 0.
        return 0;
    }
}
