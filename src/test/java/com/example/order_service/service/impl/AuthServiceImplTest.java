package com.example.order_service.service.impl;

import com.example.order_service.dto.request.auth.LoginRequest;
import com.example.order_service.dto.request.auth.RegisterRequest;
import com.example.order_service.dto.response.auth.AuthTokenResponse;
import com.example.order_service.entity.Permission;
import com.example.order_service.entity.Role;
import com.example.order_service.entity.UserAccount;
import com.example.order_service.entity.enums.RecordStatus;
import com.example.order_service.entity.enums.UserStatus;
import com.example.order_service.exception.BusinessException;
import com.example.order_service.exception.ErrorCode;
import com.example.order_service.repository.RoleRepository;
import com.example.order_service.repository.UserAccountRepository;
import com.example.order_service.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void registerCreatesUserWithDefaultRoleAndReturnsAccessToken() {
        Role userRole = role("USER", permission("ORDER_READ"));
        when(userAccountRepository.existsByEmail("demo@example.com")).thenReturn(false);
        when(roleRepository.findByCodeAndStatus("USER", RecordStatus.ACTIVE)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("{bcrypt}hash");
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount userAccount = invocation.getArgument(0);
            userAccount.setId(1L);
            return userAccount;
        });
        when(jwtTokenProvider.createAccessToken(any(UserAccount.class))).thenReturn("access-token");
        when(jwtTokenProvider.getAccessTokenTtlSeconds()).thenReturn(900L);

        AuthTokenResponse response = authService.register(new RegisterRequest(
                " Demo@Example.COM ",
                "password123",
                " Demo User ",
                " 0900000000 "
        ));

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(captor.capture());
        UserAccount savedUser = captor.getValue();

        assertThat(savedUser.getEmail()).isEqualTo("demo@example.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo("{bcrypt}hash");
        assertThat(savedUser.getFullName()).isEqualTo("Demo User");
        assertThat(savedUser.getPhone()).isEqualTo("0900000000");
        assertThat(savedUser.getRoles()).extracting(Role::getCode).containsExactly("USER");
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900L);
        assertThat(response.user().roles()).containsExactly("USER");
        assertThat(response.user().permissions()).containsExactly("ORDER_READ");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userAccountRepository.existsByEmail("demo@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest(
                "demo@example.com",
                "password123",
                null,
                null
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_EMAIL_ALREADY_EXISTS);
    }

    @Test
    void loginReturnsAccessTokenWhenPasswordMatches() {
        UserAccount userAccount = user("demo@example.com", "{bcrypt}hash", UserStatus.ACTIVE);
        when(userAccountRepository.findByEmailAndStatus("demo@example.com", RecordStatus.ACTIVE))
                .thenReturn(Optional.of(userAccount));
        when(passwordEncoder.matches("password123", "{bcrypt}hash")).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(userAccount)).thenReturn("access-token");
        when(jwtTokenProvider.getAccessTokenTtlSeconds()).thenReturn(900L);

        AuthTokenResponse response = authService.login(new LoginRequest(" Demo@Example.COM ", "password123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.user().email()).isEqualTo("demo@example.com");
    }

    @Test
    void loginRejectsWrongPasswordWithoutRevealingEmailExistence() {
        UserAccount userAccount = user("demo@example.com", "{bcrypt}hash", UserStatus.ACTIVE);
        when(userAccountRepository.findByEmailAndStatus("demo@example.com", RecordStatus.ACTIVE))
                .thenReturn(Optional.of(userAccount));
        when(passwordEncoder.matches("wrongpass", "{bcrypt}hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("demo@example.com", "wrongpass")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Invalid email or password")
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);
    }

    @Test
    void loginRejectsDisabledUser() {
        UserAccount userAccount = user("demo@example.com", "{bcrypt}hash", UserStatus.DISABLED);
        when(userAccountRepository.findByEmailAndStatus("demo@example.com", RecordStatus.ACTIVE))
                .thenReturn(Optional.of(userAccount));
        when(passwordEncoder.matches("password123", "{bcrypt}hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("demo@example.com", "password123")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_USER_DISABLED);
    }

    private UserAccount user(String email, String passwordHash, UserStatus userStatus) {
        return UserAccount.builder()
                .id(1L)
                .email(email)
                .passwordHash(passwordHash)
                .userStatus(userStatus)
                .roles(new LinkedHashSet<>(Set.of(role("USER", permission("ORDER_READ")))))
                .build();
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
