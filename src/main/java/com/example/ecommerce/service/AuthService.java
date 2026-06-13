package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.auth.LoginRequest;
import com.example.ecommerce.dto.request.auth.RegisterRequest;
import com.example.ecommerce.dto.response.auth.AuthTokenResponse;

public interface AuthService {

    AuthTokenResponse register(RegisterRequest request);

    AuthTokenResponse login(LoginRequest request);
}
