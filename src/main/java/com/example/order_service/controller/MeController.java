package com.example.order_service.controller;

import com.example.order_service.dto.response.common.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    @GetMapping
    public ApiResponse<Map<String, Object>> getCurrentUser(Authentication authentication) {
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        Map<String, Object> currentUser = Map.of(
                "username", authentication.getName(),
                "authorities", authorities
        );

        return ApiResponse.success("Current user fetched", currentUser);
    }
}

