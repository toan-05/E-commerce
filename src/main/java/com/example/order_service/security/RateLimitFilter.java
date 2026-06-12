package com.example.order_service.security;

import com.example.order_service.config.AppProperties;
import com.example.order_service.dto.response.common.ApiErrorResponse;
import com.example.order_service.exception.ErrorCode;
import com.example.order_service.security.RateLimitPolicy.RateLimitRule;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final ClientIpResolver clientIpResolver;
    private final RateLimitPolicy rateLimitPolicy;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!appProperties.isRateLimitEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<RateLimitRule> rule = rateLimitPolicy.resolve(request);
        if (rule.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = clientIpResolver.resolve(request, appProperties.isRateLimitTrustedProxyEnabled());
        WindowCounter counter = counters.computeIfAbsent(
                clientIp + ":" + rule.get().key(),
                ignored -> new WindowCounter()
        );

        if (!counter.tryConsume(rule.get())) {
            writeRateLimitExceeded(response, request, rule.get(), counter.retryAfterSeconds(rule.get()));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeRateLimitExceeded(
            HttpServletResponse response,
            HttpServletRequest request,
            RateLimitRule rule,
            long retryAfterSeconds
    ) throws IOException {
        HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setHeader("X-RateLimit-Limit", String.valueOf(rule.limit()));
        response.setHeader("X-RateLimit-Remaining", "0");
        response.setHeader(
                "X-RateLimit-Reset",
                String.valueOf(Instant.now().plusSeconds(retryAfterSeconds).getEpochSecond())
        );

        ApiErrorResponse body = new ApiErrorResponse(
                false,
                Instant.now(),
                status.value(),
                ErrorCode.RATE_LIMIT_EXCEEDED.name(),
                status.getReasonPhrase(),
                "Too many requests. Please try again later.",
                request.getRequestURI(),
                List.of()
        );
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private static final class WindowCounter {
        private Instant windowStartedAt = Instant.EPOCH;
        private int count;

        synchronized boolean tryConsume(RateLimitRule rule) {
            Instant now = Instant.now();
            if (!now.isBefore(windowStartedAt.plus(rule.window()))) {
                windowStartedAt = now;
                count = 0;
            }

            if (count >= rule.limit()) {
                return false;
            }

            count++;
            return true;
        }

        synchronized long retryAfterSeconds(RateLimitRule rule) {
            long seconds = windowStartedAt.plus(rule.window()).getEpochSecond() - Instant.now().getEpochSecond();
            return Math.max(seconds, 1);
        }
    }
}
