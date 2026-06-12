package com.example.order_service.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ClientIpResolver {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String X_REAL_IP = "X-Real-IP";

    public String resolve(HttpServletRequest request, boolean trustedProxyEnabled) {
        if (trustedProxyEnabled) {
            String forwardedFor = request.getHeader(X_FORWARDED_FOR);
            if (StringUtils.hasText(forwardedFor)) {
                return forwardedFor.split(",")[0].trim();
            }

            String realIp = request.getHeader(X_REAL_IP);
            if (StringUtils.hasText(realIp)) {
                return realIp.trim();
            }
        }

        return request.getRemoteAddr();
    }
}
