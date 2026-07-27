package com.ridvankarsli.sagliktanapi.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {

    @Test
    void allowsRequestsUpToLimitThenRejects() {
        RateLimiter limiter = new RateLimiter();
        Duration window = Duration.ofMinutes(1);
        String key = "ip-1:/api/auth/login";

        assertTrue(limiter.tryConsume(key, 3, window));
        assertTrue(limiter.tryConsume(key, 3, window));
        assertTrue(limiter.tryConsume(key, 3, window));
        assertFalse(limiter.tryConsume(key, 3, window));
    }

    @Test
    void differentKeysHaveIndependentLimits() {
        RateLimiter limiter = new RateLimiter();
        Duration window = Duration.ofMinutes(1);

        assertTrue(limiter.tryConsume("ip-1:/api/auth/login", 1, window));
        assertFalse(limiter.tryConsume("ip-1:/api/auth/login", 1, window));
        // Farklı bir IP aynı anda kendi limitini kullanabilmeli.
        assertTrue(limiter.tryConsume("ip-2:/api/auth/login", 1, window));
    }
}
