package com.example.ecommerce.security;

import com.example.ecommerce.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RateLimitPolicy {

    private final AppProperties appProperties;

    public Optional<RateLimitRule> resolve(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        if (HttpMethod.POST.matches(method) && "/api/v1/auth/login".equals(path)) {
            return Optional.of(new RateLimitRule(
                    "AUTH_LOGIN",
                    appProperties.getRateLimitLoginLimitPerMinute(),
                    Duration.ofMinutes(1)
            ));
        }

        if (HttpMethod.POST.matches(method) && "/api/v1/auth/register".equals(path)) {
            return Optional.of(new RateLimitRule(
                    "AUTH_REGISTER",
                    appProperties.getRateLimitRegisterLimitPerMinute(),
                    Duration.ofMinutes(1)
            ));
        }

        return Optional.empty();
    }

    public record RateLimitRule(
            String key,
            int limit,
            Duration window
    ) {
    }
}
