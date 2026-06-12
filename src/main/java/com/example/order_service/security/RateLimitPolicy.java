package com.example.order_service.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class RateLimitPolicy {

    public Optional<RateLimitRule> resolve(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        if (HttpMethod.POST.matches(method) && "/api/v1/auth/login".equals(path)) {
            return Optional.of(new RateLimitRule("AUTH_LOGIN", 5, Duration.ofMinutes(1)));
        }

        if (HttpMethod.POST.matches(method) && "/api/v1/auth/register".equals(path)) {
            return Optional.of(new RateLimitRule("AUTH_REGISTER", 3, Duration.ofMinutes(1)));
        }

        return Optional.empty();
    }
}
