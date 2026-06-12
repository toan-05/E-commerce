package com.example.order_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final ClientIpResolver clientIpResolver;
    private final RateLimitPolicy rateLimitPolicy;
    private final boolean trustedProxyEnabled;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // Actual token-bucket enforcement will be wired in the auth implementation phase.
        clientIpResolver.resolve(request, trustedProxyEnabled);
        rateLimitPolicy.resolve(request);
        filterChain.doFilter(request, response);
    }
}
