package com.example.ecommerce.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternalSetsAuthenticationFromBearerToken() throws ServletException, IOException {
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        when(jwtTokenProvider.validateAccessToken("token")).thenReturn(new CurrentUser(
                1L,
                "demo@example.com",
                List.of("USER"),
                List.of("ORDER_READ")
        ));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo(new CurrentUser(1L, "demo@example.com", List.of("USER"), List.of("ORDER_READ")));
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "PERMISSION_ORDER_READ");
    }
}
