package com.example.ecommerce.security;

import com.example.ecommerce.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitFilterTest {

    @Test
    void doFilterInternalRejectsRequestAfterLimitExceeded() throws Exception {
        AppProperties appProperties = mock(AppProperties.class);
        when(appProperties.isRateLimitEnabled()).thenReturn(true);
        when(appProperties.isRateLimitTrustedProxyEnabled()).thenReturn(false);
        when(appProperties.getRateLimitLoginLimitPerMinute()).thenReturn(2);

        RateLimitFilter filter = new RateLimitFilter(
                new ClientIpResolver(),
                new RateLimitPolicy(appProperties),
                appProperties,
                new ObjectMapper().registerModule(new JavaTimeModule())
        );

        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(loginRequest(), firstResponse, new MockFilterChain());

        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(loginRequest(), secondResponse, new MockFilterChain());

        MockHttpServletResponse thirdResponse = new MockHttpServletResponse();
        filter.doFilter(loginRequest(), thirdResponse, new MockFilterChain());

        assertThat(firstResponse.getStatus()).isEqualTo(200);
        assertThat(secondResponse.getStatus()).isEqualTo(200);
        assertThat(thirdResponse.getStatus()).isEqualTo(429);
        assertThat(thirdResponse.getHeader("Retry-After")).isNotBlank();
        assertThat(thirdResponse.getHeader("X-RateLimit-Limit")).isEqualTo("2");
        assertThat(thirdResponse.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(thirdResponse.getHeader("X-RateLimit-Reset")).isNotBlank();
        assertThat(thirdResponse.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
    }

    private MockHttpServletRequest loginRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("127.0.0.1");
        return request;
    }
}
