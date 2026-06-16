package com.naro.auth_service.service;

import com.naro.auth_service.dto.AuthResponse;
import com.naro.auth_service.dto.LoginRequest;
import com.naro.auth_service.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request, HttpServletResponse response);

    AuthResponse login(LoginRequest request, HttpServletResponse response);

    AuthResponse refresh(HttpServletRequest request, HttpServletResponse response);

    void logout(HttpServletRequest request, HttpServletResponse response);

}
