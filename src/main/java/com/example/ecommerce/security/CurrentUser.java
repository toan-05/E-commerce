package com.example.ecommerce.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.stream.Stream;

public record CurrentUser(
        Long id,
        String email,
        List<String> roles,
        List<String> permissions
) {

    public List<SimpleGrantedAuthority> authorities() {
        return Stream.concat(
                        roles.stream().map(role -> "ROLE_" + role),
                        permissions.stream().map(permission -> "PERMISSION_" + permission)
                )
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }
}
