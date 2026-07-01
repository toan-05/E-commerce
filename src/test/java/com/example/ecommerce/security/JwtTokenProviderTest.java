package com.example.ecommerce.security;

import com.example.ecommerce.config.AppProperties;
import com.example.ecommerce.entity.Permission;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.UserAccount;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtTokenProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String SECRET = "dev-secret-change-me";

    @Test
    void createAccessTokenIncludesUserRolesAndPermissions() throws Exception {
        AppProperties appProperties = mock(AppProperties.class);
        when(appProperties.getAuthIssuer()).thenReturn("order-service");
        when(appProperties.getAuthAccessTokenTtlSeconds()).thenReturn(900L);
        when(appProperties.getAuthJwtSecret()).thenReturn(SECRET);
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(appProperties, objectMapper);

        UserAccount userAccount = UserAccount.builder()
                .id(1L)
                .email("demo@example.com")
                .roles(new LinkedHashSet<>(Set.of(role("USER", permission("ORDER_READ")))))
                .build();

        String token = jwtTokenProvider.createAccessToken(userAccount);
        String[] parts = token.split("\\.");
        Map<String, Object> header = objectMapper.readValue(
                new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8),
                new TypeReference<>() {
                }
        );
        Map<String, Object> payload = objectMapper.readValue(
                new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8),
                new TypeReference<>() {
                }
        );

        assertThat(parts).hasSize(3);
        assertThat(header.get("alg")).isEqualTo("HS512");
        assertThat(header.get("typ")).isEqualTo("JWT");
        assertThat(payload.get("iss")).isEqualTo("order-service");
        assertThat(payload.get("sub")).isEqualTo("1");
        assertThat(payload.get("email")).isEqualTo("demo@example.com");
        assertThat(payload.get("roles")).isEqualTo(List.of("USER"));
        assertThat(payload.get("permissions")).isEqualTo(List.of("ORDER_READ"));
        assertThat(payload).containsKeys("iat", "exp", "jti");

        CurrentUser currentUser = jwtTokenProvider.validateAccessToken(token);
        assertThat(currentUser.id()).isEqualTo(1L);
        assertThat(currentUser.email()).isEqualTo("demo@example.com");
        assertThat(currentUser.roles()).containsExactly("USER");
        assertThat(currentUser.permissions()).containsExactly("ORDER_READ");
    }

    @Test
    void validateAccessTokenRejectsTamperedToken() {
        AppProperties appProperties = mock(AppProperties.class);
        when(appProperties.getAuthIssuer()).thenReturn("order-service");
        when(appProperties.getAuthAccessTokenTtlSeconds()).thenReturn(900L);
        when(appProperties.getAuthJwtSecret()).thenReturn(SECRET);
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(appProperties, objectMapper);

        UserAccount userAccount = UserAccount.builder()
                .id(1L)
                .email("demo@example.com")
                .roles(new LinkedHashSet<>(Set.of(role("USER", permission("ORDER_READ")))))
                .build();

        String token = jwtTokenProvider.createAccessToken(userAccount);
        String tamperedToken = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> jwtTokenProvider.validateAccessToken(tamperedToken))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_TOKEN_INVALID);
    }

    @Test
    void validateAccessTokenRejectsExpiredToken() {
        JwtTokenProvider jwtTokenProvider = jwtTokenProvider("order-service", SECRET, 900L);
        String token = signedToken(
                Map.of("alg", "HS512", "typ", "JWT"),
                Map.of(
                        "iss", "order-service",
                        "sub", "1",
                        "email", "demo@example.com",
                        "roles", List.of("USER"),
                        "permissions", List.of("ORDER_READ"),
                        "iat", Instant.now().minusSeconds(120).getEpochSecond(),
                        "exp", Instant.now().minusSeconds(60).getEpochSecond(),
                        "jti", "token-id"
                ),
                SECRET
        );

        assertThatThrownBy(() -> jwtTokenProvider.validateAccessToken(token))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_TOKEN_EXPIRED);
    }

    @Test
    void validateAccessTokenRejectsInvalidIssuer() {
        JwtTokenProvider jwtTokenProvider = jwtTokenProvider("order-service", SECRET, 900L);
        String token = signedToken(
                Map.of("alg", "HS512", "typ", "JWT"),
                validPayload(Map.of("iss", "other-service")),
                SECRET
        );

        assertThatThrownBy(() -> jwtTokenProvider.validateAccessToken(token))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_TOKEN_INVALID);
    }

    @Test
    void validateAccessTokenRejectsInvalidAlgorithm() {
        JwtTokenProvider jwtTokenProvider = jwtTokenProvider("order-service", SECRET, 900L);
        String token = signedToken(
                Map.of("alg", "HS256", "typ", "JWT"),
                validPayload(Map.of()),
                SECRET
        );

        assertThatThrownBy(() -> jwtTokenProvider.validateAccessToken(token))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_TOKEN_INVALID);
    }

    @Test
    void validateAccessTokenRejectsMissingEmail() {
        JwtTokenProvider jwtTokenProvider = jwtTokenProvider("order-service", SECRET, 900L);
        String token = signedToken(
                Map.of("alg", "HS512", "typ", "JWT"),
                Map.of(
                        "iss", "order-service",
                        "sub", "1",
                        "roles", List.of("USER"),
                        "permissions", List.of("ORDER_READ"),
                        "iat", Instant.now().getEpochSecond(),
                        "exp", Instant.now().plusSeconds(900).getEpochSecond(),
                        "jti", "token-id"
                ),
                SECRET
        );

        assertThatThrownBy(() -> jwtTokenProvider.validateAccessToken(token))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_TOKEN_INVALID);
    }

    @Test
    void validateAccessTokenRejectsNonStringPermission() {
        JwtTokenProvider jwtTokenProvider = jwtTokenProvider("order-service", SECRET, 900L);
        String token = signedToken(
                Map.of("alg", "HS512", "typ", "JWT"),
                validPayload(Map.of("permissions", List.of("ORDER_READ", 123))),
                SECRET
        );

        assertThatThrownBy(() -> jwtTokenProvider.validateAccessToken(token))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_TOKEN_INVALID);
    }

    private Role role(String code, Permission permission) {
        return Role.builder()
                .id(1L)
                .code(code)
                .name(code)
                .permissions(new LinkedHashSet<>(Set.of(permission)))
                .build();
    }

    private Permission permission(String code) {
        return Permission.builder()
                .id(1L)
                .code(code)
                .name(code)
                .build();
    }

    private JwtTokenProvider jwtTokenProvider(String issuer, String secret, long ttlSeconds) {
        AppProperties appProperties = mock(AppProperties.class);
        when(appProperties.getAuthIssuer()).thenReturn(issuer);
        when(appProperties.getAuthAccessTokenTtlSeconds()).thenReturn(ttlSeconds);
        when(appProperties.getAuthJwtSecret()).thenReturn(secret);
        return new JwtTokenProvider(appProperties, objectMapper);
    }

    private Map<String, Object> validPayload(Map<String, Object> overrides) {
        java.util.HashMap<String, Object> payload = new java.util.HashMap<>();
        payload.put("iss", "order-service");
        payload.put("sub", "1");
        payload.put("email", "demo@example.com");
        payload.put("roles", List.of("USER"));
        payload.put("permissions", List.of("ORDER_READ"));
        payload.put("iat", Instant.now().getEpochSecond());
        payload.put("exp", Instant.now().plusSeconds(900).getEpochSecond());
        payload.put("jti", "token-id");
        payload.putAll(overrides);
        return payload;
    }

    private String signedToken(Map<String, Object> header, Map<String, Object> payload, String secret) {
        try {
            String unsignedToken = base64Url(objectMapper.writeValueAsBytes(header))
                    + "."
                    + base64Url(objectMapper.writeValueAsBytes(payload));
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return unsignedToken + "." + base64Url(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
