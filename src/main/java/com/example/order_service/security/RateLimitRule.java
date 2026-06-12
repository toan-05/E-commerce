package com.example.order_service.security;

import java.time.Duration;

public record RateLimitRule(
        String key,
        int limit,
        Duration window
) {
}
