package com.example.order_service.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class AppProperties {

    @Value("${app.instance-name}")
    private String instanceName;

    @Value("${app.kafka.order-created-topic}")
    private String orderCreatedTopic;

    @Value("${app.kafka.inventory-result-topic}")
    private String inventoryResultTopic;

    @Value("${app.auth.issuer}")
    private String authIssuer;

    @Value("${app.auth.access-token-ttl-seconds}")
    private long authAccessTokenTtlSeconds;

    @Value("${app.auth.jwt-secret}")
    private String authJwtSecret;

    @Value("${app.rate-limit.enabled}")
    private boolean rateLimitEnabled;

    @Value("${app.rate-limit.trusted-proxy-enabled}")
    private boolean rateLimitTrustedProxyEnabled;

    @Value("${app.rate-limit.login-limit-per-minute}")
    private int rateLimitLoginLimitPerMinute;

    @Value("${app.rate-limit.register-limit-per-minute}")
    private int rateLimitRegisterLimitPerMinute;
}
