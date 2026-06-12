package com.example.order_service.service;

import com.example.order_service.dto.request.auth.LoginRequest;
import com.example.order_service.dto.request.auth.RegisterRequest;
import com.example.order_service.dto.response.auth.AuthTokenResponse;

public interface AuthService {

    AuthTokenResponse register(RegisterRequest request);

    AuthTokenResponse login(LoginRequest request);
}
