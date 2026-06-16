package com.naro.auth_service.service;

import com.naro.auth_service.dto.AuthResponse;
import com.naro.auth_service.dto.LoginRequest;
import com.naro.auth_service.dto.RegisterRequest;
import com.naro.auth_service.entity.RefreshToken;
import com.naro.auth_service.entity.Role;
import com.naro.auth_service.entity.User;
import com.naro.auth_service.repository.UserRepository;
import com.naro.auth_service.security.CookieUtil;
import com.naro.auth_service.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    @Value("${application.security.jwt.expiration}")
    private Long jwtExpiration;

    @Value("${application.security.jwt.refresh-expiration}")
    private Long refreshExpiration;

    @Value("${application.security.cookie.secure:false}")
    private boolean cookieSecure;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletResponse response) {
        if(!request.getPassword().equals(request.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las contraseñas no coinciden");
        }

        if(userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El email ya está registrado");
        }

        User user = User.builder()
            .nombre(request.getNombre())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(Role.ROLE_USER)
            .build();

        userRepository.save(user);

        issueTokenCookie(user, response);

        return toAuthResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));
        issueTokenCookie(user, response);
        return toAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshTokenValue = CookieUtil
            .getCookieValue(request, CookieUtil.REFRESH_TOKEN_COOKIE)
            .orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token no encontrado"));

        RefreshToken refreshToken = refreshTokenService.verify(refreshTokenValue);
        User user = refreshToken.getUser();

        issueTokenCookie(user, response);
        return toAuthResponse(user);
    }

    @Override
    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {

        CookieUtil.getCookieValue(request, CookieUtil.REFRESH_TOKEN_COOKIE)
            .flatMap(refreshTokenService::findByToken)
            .filter(rt -> !rt.isRevoked())
            .ifPresent(rt -> refreshTokenService.revokeAllByUser(rt.getUser()));

        CookieUtil.clearCookie(response, CookieUtil.ACCESS_TOKEN_COOKIE, cookieSecure);
        CookieUtil.clearCookie(response, CookieUtil.REFRESH_TOKEN_COOKIE, cookieSecure);

    }

    private void issueTokenCookie(User user, HttpServletResponse response) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("nombre", user.getNombre());
        String accessToken = jwtService.generateToken(extraClaims, user);
        RefreshToken refreshToken = refreshTokenService.create(user);

        CookieUtil.addCookie(response, CookieUtil.ACCESS_TOKEN_COOKIE, accessToken, jwtExpiration / 1000, cookieSecure);
        CookieUtil.addCookie(response, CookieUtil.REFRESH_TOKEN_COOKIE, refreshToken.getToken(), refreshExpiration / 1000, cookieSecure);
    }

    private AuthResponse toAuthResponse(User user) {
        return AuthResponse.builder()
            .id(user.getId())
            .nombre(user.getNombre())
            .email(user.getEmail())
            .role(user.getRole().name())
            .build();
    }

}
