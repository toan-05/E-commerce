package com.example.order_service.controller;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    @GetMapping
    public Map<String, Object> getCurrentUser(JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Map.of(
                "subject", jwt.getSubject(),
                "username", authentication.getName(),
                "authorities", authorities,
                "claims", jwt.getClaims()
        );
    }
}

