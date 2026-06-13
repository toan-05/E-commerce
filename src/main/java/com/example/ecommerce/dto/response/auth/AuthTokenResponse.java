package com.example.ecommerce.dto.response.auth;

public record AuthTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        AuthUserResponse user
) {

    public static AuthTokenResponse bearer(String accessToken, long expiresIn, AuthUserResponse user) {
        return new AuthTokenResponse(accessToken, "Bearer", expiresIn, user);
    }
}
