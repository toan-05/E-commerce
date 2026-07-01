package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.auth.LoginRequest;
import com.example.ecommerce.dto.request.auth.RegisterRequest;
import com.example.ecommerce.dto.response.auth.AuthTokenResponse;
import com.example.ecommerce.dto.response.auth.AuthUserResponse;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ErrorCode;
import com.example.ecommerce.exception.GlobalExceptionHandler;
import com.example.ecommerce.security.CurrentUser;
import com.example.ecommerce.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AuthService authService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void registerReturnsTokenResponse() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(tokenResponse());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "demo@example.com",
                                "password123",
                                "Demo User",
                                "0900000000"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value("demo@example.com"));
    }

    @Test
    void registerRejectsInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "not-an-email",
                                "short",
                                null,
                                null
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void registerReturnsConflictWhenEmailAlreadyExists() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenThrow(new BusinessException(
                ErrorCode.AUTH_EMAIL_ALREADY_EXISTS,
                "Email already exists"
        ));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "demo@example.com",
                                "password123",
                                null,
                                null
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTH_EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void loginReturnsTokenResponse() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(tokenResponse());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new LoginRequest(
                                "demo@example.com",
                                "password123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.user.roles[0]").value("USER"));
    }

    @Test
    void loginReturnsUnauthorizedWhenCredentialsAreInvalid() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new BusinessException(
                ErrorCode.AUTH_INVALID_CREDENTIALS,
                "Invalid email or password"
        ));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new LoginRequest(
                                "demo@example.com",
                                "password123"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    void meReturnsCurrentUserFromAuthentication() throws Exception {
        CurrentUser currentUser = new CurrentUser(
                1L,
                "demo@example.com",
                List.of("USER"),
                List.of("ORDER_READ")
        );
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                currentUser,
                null,
                currentUser.authorities()
        );

        mockMvc.perform(get("/api/v1/auth/me").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.email").value("demo@example.com"))
                .andExpect(jsonPath("$.data.roles[*]", containsInAnyOrder("USER")))
                .andExpect(jsonPath("$.data.permissions[*]", containsInAnyOrder("ORDER_READ")));
    }

    private AuthTokenResponse tokenResponse() {
        return AuthTokenResponse.bearer(
                "access-token",
                900L,
                new AuthUserResponse(
                        1L,
                        "demo@example.com",
                        "Demo User",
                        List.of("USER"),
                        List.of("ORDER_READ")
                )
        );
    }
}
