package com.example.order_service.security;

import com.example.order_service.config.AppProperties;
import com.example.order_service.entity.Permission;
import com.example.order_service.entity.Role;
import com.example.order_service.entity.UserAccount;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtTokenProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createAccessTokenIncludesUserRolesAndPermissions() throws Exception {
        AppProperties appProperties = mock(AppProperties.class);
        when(appProperties.getAuthIssuer()).thenReturn("order-service");
        when(appProperties.getAuthAccessTokenTtlSeconds()).thenReturn(900L);
        when(appProperties.getAuthJwtSecret()).thenReturn("dev-secret-change-me");
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
}
