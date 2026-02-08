package com.url_shortner.project.service.ratelimit;

public interface RateLimiterStrategy {
    /**
     * Checks if the request is allowed.
     *
     * @param key           The unique key for the user/IP.
     * @param limit         The maximum number of requests allowed.
     * @param windowSeconds The time window in seconds.
     * @return true if allowed, false if blocked.
     */
    boolean isAllowed(String key, long limit, long windowSeconds);

    /**
     * Returns the remaining requests allowed in the current window.
     * Note: This may be approximate depending on the strategy.
     *
     * @param key           The unique key.
     * @param limit         The max limit.
     * @param windowSeconds The window size.
     * @return Remaining requests.
     */
    long getRemaining(String key, long limit, long windowSeconds);
}
