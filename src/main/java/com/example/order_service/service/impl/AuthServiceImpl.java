package com.example.order_service.service.impl;

import com.example.order_service.dto.request.auth.LoginRequest;
import com.example.order_service.dto.request.auth.RegisterRequest;
import com.example.order_service.dto.response.auth.AuthTokenResponse;
import com.example.order_service.dto.response.auth.AuthUserResponse;
import com.example.order_service.entity.Role;
import com.example.order_service.entity.UserAccount;
import com.example.order_service.entity.enums.RecordStatus;
import com.example.order_service.exception.BusinessException;
import com.example.order_service.exception.ErrorCode;
import com.example.order_service.repository.RoleRepository;
import com.example.order_service.repository.UserAccountRepository;
import com.example.order_service.security.JwtTokenProvider;
import com.example.order_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_ROLE_CODE = "USER";
    private static final int BCRYPT_MAX_PASSWORD_BYTES = 72;

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public AuthTokenResponse register(RegisterRequest request) {
        validateBcryptPasswordLength(request.password());
        String email = normalizeEmail(request.email());

        if (userAccountRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_ALREADY_EXISTS, "Email already exists");
        }

        Role defaultRole = roleRepository.findByCodeAndStatus(DEFAULT_ROLE_CODE, RecordStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.AUTH_DEFAULT_ROLE_NOT_FOUND,
                        "Default user role is not configured"
                ));

        UserAccount userAccount = UserAccount.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(normalizeOptional(request.fullName()))
                .phone(normalizeOptional(request.phone()))
                .roles(new LinkedHashSet<>(Set.of(defaultRole)))
                .build();

        UserAccount savedUser = userAccountRepository.save(userAccount);
        return tokenResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthTokenResponse login(LoginRequest request) {
        validateBcryptPasswordLength(request.password());
        String email = normalizeEmail(request.email());

        UserAccount userAccount = userAccountRepository.findByEmailAndStatus(email, RecordStatus.ACTIVE)
                .orElseThrow(this::invalidCredentials);

        if (!passwordEncoder.matches(request.password(), userAccount.getPasswordHash())) {
            throw invalidCredentials();
        }

        if (!userAccount.isEnabled()) {
            throw new BusinessException(ErrorCode.AUTH_USER_DISABLED, "User account is disabled");
        }

        return tokenResponse(userAccount);
    }

    private AuthTokenResponse tokenResponse(UserAccount userAccount) {
        return AuthTokenResponse.bearer(
                jwtTokenProvider.createAccessToken(userAccount),
                jwtTokenProvider.getAccessTokenTtlSeconds(),
                AuthUserResponse.from(userAccount)
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void validateBcryptPasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    "Password must not exceed 72 bytes"
            );
        }
    }

    private BusinessException invalidCredentials() {
        return new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid email or password");
    }
}
