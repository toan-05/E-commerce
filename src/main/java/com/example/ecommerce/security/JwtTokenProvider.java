package com.example.ecommerce.security;

import com.example.ecommerce.config.AppProperties;
import com.example.ecommerce.entity.UserAccount;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String JWT_ALGORITHM = "HS512";
    private static final String JWT_TYPE = "JWT";
    private static final String HMAC_SHA512 = "HmacSHA512";

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public String createAccessToken(UserAccount userAccount) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(appProperties.getAuthAccessTokenTtlSeconds());

        Map<String, Object> header = Map.of(
                "alg", JWT_ALGORITHM,
                "typ", JWT_TYPE
        );
        Map<String, Object> payload = Map.of(
                "iss", appProperties.getAuthIssuer(),
                "sub", String.valueOf(userAccount.getId()),
                "email", userAccount.getEmail(),
                "roles", userAccount.getRoleCodes(),
                "permissions", userAccount.getPermissionCodes(),
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

    public CurrentUser validateAccessToken(String token) {
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) {
            throw invalidToken();
        }

        String unsignedToken = parts[0] + "." + parts[1];
        byte[] expectedSignature = sign(unsignedToken).getBytes(StandardCharsets.UTF_8);
        byte[] actualSignature = parts[2].getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
            throw invalidToken();
        }

        Map<String, Object> header = decodeJson(parts[0]);
        if (!JWT_ALGORITHM.equals(header.get("alg")) || !JWT_TYPE.equals(header.get("typ"))) {
            throw invalidToken();
        }

        Map<String, Object> payload = decodeJson(parts[1]);
        if (!appProperties.getAuthIssuer().equals(payload.get("iss"))) {
            throw invalidToken();
        }

        long expiresAt = numberClaim(payload, "exp");
        if (Instant.now().getEpochSecond() >= expiresAt) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_EXPIRED, "Access token is expired");
        }

        return new CurrentUser(
                longClaim(payload, "sub"),
                stringClaim(payload, "email"),
                stringListClaim(payload, "roles"),
                stringListClaim(payload, "permissions")
        );
    }

    private String base64UrlJson(Object value) {
        try {
            return base64Url(objectMapper.writeValueAsBytes(value));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize JWT content", ex);
        }
    }

    private Map<String, Object> decodeJson(String value) {
        try {
            return objectMapper.readValue(
                    Base64.getUrlDecoder().decode(value),
                    new TypeReference<>() {
                    }
            );
        } catch (Exception ex) {
            throw invalidToken();
        }
    }

    private String stringClaim(Map<String, Object> payload, String claimName) {
        Object value = payload.get(claimName);
        if (!(value instanceof String stringValue) || stringValue.isBlank()) {
            throw invalidToken();
        }
        return stringValue;
    }

    private long longClaim(Map<String, Object> payload, String claimName) {
        try {
            return Long.parseLong(stringClaim(payload, claimName));
        } catch (NumberFormatException ex) {
            throw invalidToken();
        }
    }

    private long numberClaim(Map<String, Object> payload, String claimName) {
        Object value = payload.get(claimName);
        if (!(value instanceof Number numberValue)) {
            throw invalidToken();
        }
        return numberValue.longValue();
    }

    private List<String> stringListClaim(Map<String, Object> payload, String claimName) {
        Object value = payload.get(claimName);
        if (!(value instanceof List<?> listValue)) {
            return Collections.emptyList();
        }
        if (!listValue.stream().allMatch(String.class::isInstance)) {
            throw invalidToken();
        }

        return listValue.stream()
                .map(String.class::cast)
                .toList();
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

    private BusinessException invalidToken() {
        return new BusinessException(ErrorCode.AUTH_TOKEN_INVALID, "Access token is invalid");
    }
}
