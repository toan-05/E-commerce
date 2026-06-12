package com.example.order_service.dto.response.auth;

import com.example.order_service.entity.Permission;
import com.example.order_service.entity.Role;
import com.example.order_service.entity.UserAccount;

import java.util.List;

public record AuthUserResponse(
        Long id,
        String email,
        String fullName,
        List<String> roles,
        List<String> permissions
) {

    public static AuthUserResponse from(UserAccount userAccount) {
        List<String> roles = userAccount.getRoles().stream()
                .map(Role::getCode)
                .sorted()
                .toList();

        List<String> permissions = userAccount.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .distinct()
                .sorted()
                .toList();

        return new AuthUserResponse(
                userAccount.getId(),
                userAccount.getEmail(),
                userAccount.getFullName(),
                roles,
                permissions
        );
    }
}
