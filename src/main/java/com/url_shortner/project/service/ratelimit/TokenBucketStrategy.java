package com.url_shortner.project.service.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Component("tokenBucketStrategy")
@RequiredArgsConstructor
public class TokenBucketStrategy implements RateLimiterStrategy {

    private final StringRedisTemplate redisTemplate;

    // Lua script for atomic Token Bucket refilling and consumption
    private final String luaScript = "local key = KEYS[1]\n" +
            "local limit = tonumber(ARGV[1])\n" +
            "local rate = tonumber(ARGV[2])\n" + // tokens per second
            "local now = tonumber(ARGV[3])\n" +

            "local tokens_key = key .. ':tokens'\n" +
            "local timestamp_key = key .. ':ts'\n" +

            "local current_tokens = tonumber(redis.call('get', tokens_key))\n" +
            "local last_refill = tonumber(redis.call('get', timestamp_key))\n" +

            "if current_tokens == nil then\n" +
            "   current_tokens = limit\n" +
            "   last_refill = now\n" +
            "end\n" +

            "local delta = math.max(0, now - last_refill)\n" +
            "local filled_tokens = math.min(limit, current_tokens + (delta * rate))\n" +
            "filled_tokens = math.floor(filled_tokens)\n" +

            "if filled_tokens >= 1 then\n" +
            "   local new_tokens = filled_tokens - 1\n" +
            "   redis.call('set', tokens_key, new_tokens)\n" +
            "   redis.call('set', timestamp_key, now)\n" +
            "   redis.call('expire', tokens_key, 60)\n" +
            "   redis.call('expire', timestamp_key, 60)\n" +
            "   return new_tokens\n" +
            "else\n" +
            "   return -1\n" +
            "end";

    private final RedisScript<Long> script = new DefaultRedisScript<>(luaScript, Long.class);

    @Override
    public boolean isAllowed(String key, long limit, long windowSeconds) {
        String redisKey = "rate_limit:token_bucket:" + key;
        long now = Instant.now().getEpochSecond();
        // Calculate rate: tokens per second. If windowSeconds is 60 and limit is 60,
        // rate is 1.
        // If windowSeconds is 1 and limit is 10, rate is 10.
        // Using float for rate might be needed for slow rates, but Lua handles numbers.
        // For simplicity, we assume rate >= 1 token/sec or we handle fractional
        // refilling logic more complexly.
        // To keep it simple: Rate = limit / windowSeconds.
        double rate = (double) limit / windowSeconds;

        List<String> keys = Collections.singletonList(redisKey);
        // Execute Lua script
        Long result = redisTemplate.execute(script, keys, String.valueOf(limit), String.valueOf(rate),
                String.valueOf(now));

        return result != null && result >= 0;
    }

    @Override
    public long getRemaining(String key, long limit, long windowSeconds) {
        // Approximate
        String tokensKey = "rate_limit:token_bucket:" + key + ":tokens";
        String val = redisTemplate.opsForValue().get(tokensKey);
        return val != null ? Double.valueOf(val).longValue() : limit;
    }
}
