package com.example.ecommerce.dto.response.auth;

import com.example.ecommerce.entity.UserAccount;
import com.example.ecommerce.security.CurrentUser;

import java.util.List;

public record AuthUserResponse(
        Long id,
        String email,
        String fullName,
        List<String> roles,
        List<String> permissions
) {

    public static AuthUserResponse from(UserAccount userAccount) {
        return new AuthUserResponse(
                userAccount.getId(),
                userAccount.getEmail(),
                userAccount.getFullName(),
                userAccount.getRoleCodes(),
                userAccount.getPermissionCodes()
        );
    }

    public static AuthUserResponse from(CurrentUser currentUser) {
        return new AuthUserResponse(
                currentUser.id(),
                currentUser.email(),
                null,
                currentUser.roles(),
                currentUser.permissions()
        );
    }
}
