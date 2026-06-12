package com.example.order_service.security;

import com.example.order_service.config.AppProperties;
import com.example.order_service.entity.Permission;
import com.example.order_service.entity.Role;
import com.example.order_service.entity.UserAccount;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String HMAC_SHA512 = "HmacSHA512";

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public String createAccessToken(UserAccount userAccount) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(appProperties.getAuthAccessTokenTtlSeconds());

        Map<String, Object> header = Map.of(
                "alg", "HS512",
                "typ", "JWT"
        );
        Map<String, Object> payload = Map.of(
                "iss", appProperties.getAuthIssuer(),
                "sub", String.valueOf(userAccount.getId()),
                "email", userAccount.getEmail(),
                "roles", roleCodes(userAccount),
                "permissions", permissionCodes(userAccount),
                "iat", issuedAt.getEpochSecond(),
                "exp", expiresAt.getEpochSecond(),
                "jti", UUID.randomUUID().toString()
        );

        String unsignedToken = base64UrlJson(header) + "." + base64UrlJson(payload);
        return unsignedToken + "." + sign(unsignedToken);
    }

    public long getAccessTokenTtlSeconds() {
        return appProperties.getAuthAccessTokenTtlSeconds();
    }

    private List<String> roleCodes(UserAccount userAccount) {
        return userAccount.getRoles().stream()
                .map(Role::getCode)
                .sorted()
                .toList();
    }

    private List<String> permissionCodes(UserAccount userAccount) {
        return userAccount.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .distinct()
                .sorted()
                .toList();
    }

    private String base64UrlJson(Object value) {
        try {
            return base64Url(objectMapper.writeValueAsBytes(value));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize JWT content", ex);
        }
    }

    private String sign(String unsignedToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA512);
            SecretKeySpec key = new SecretKeySpec(
                    appProperties.getAuthJwtSecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA512
            );
            mac.init(key);
            return base64Url(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not sign JWT", ex);
        }
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
