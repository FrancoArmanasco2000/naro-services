package com.naro.auth_service.service;

import com.naro.auth_service.config.JwtProperties;
import com.naro.auth_service.dto.AuthResponse;
import com.naro.auth_service.dto.CrearUsuarioRequest;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;
    private final RestClient userServiceRestClient;

    @Value("${application.security.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${application.security.internal-secret}")
    private String internalSecret;

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

        crearPerfilUsuario(user, request);

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
            .ifPresent(rt -> refreshTokenService.deleteAllByUser(rt.getUser()));

        CookieUtil.clearCookie(response, CookieUtil.ACCESS_TOKEN_COOKIE, cookieSecure);
        CookieUtil.clearCookie(response, CookieUtil.REFRESH_TOKEN_COOKIE, cookieSecure);

    }

    private void crearPerfilUsuario(User user, RegisterRequest request) {
        CrearUsuarioRequest perfilRequest = CrearUsuarioRequest.builder()
            .id(user.getId())
            .nombre(request.getNombre())
            .apellido(request.getApellido())
            .dni(request.getDni())
            .fechaNacimiento(request.getFechaNacimiento())
            .genero(request.getGenero())
            .email(request.getEmail())
            .ciudad(request.getCiudad())
            .provincia(request.getProvincia())
            .pais(request.getPais())
            .build();

        try {
            userServiceRestClient.post()
                .uri("/api/usuarios")
                .header("X-Internal-Secret", internalSecret)
                .body(perfilRequest)
                .retrieve()
                .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            // user-service rechazo los datos (ej. DNI duplicado) -> no es un
            // problema transitorio, reintentar no lo va a arreglar.
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No se pudo completar el registro: el DNI ingresado ya está en uso",
                e
            );
        } catch (RestClientException e) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo completar el registro, intentá de nuevo en unos minutos",
                e
            );
        }
    }

    private void issueTokenCookie(User user, HttpServletResponse response) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("id", user.getId());
        extraClaims.put("role", user.getRole().name());
        String accessToken = jwtService.generateToken(extraClaims, user);
        RefreshToken refreshToken = refreshTokenService.create(user);

        CookieUtil.addCookie(response, CookieUtil.ACCESS_TOKEN_COOKIE, accessToken, jwtProperties.getExpiration() / 1000, cookieSecure);
        CookieUtil.addCookie(response, CookieUtil.REFRESH_TOKEN_COOKIE, refreshToken.getToken(), jwtProperties.getRefreshExpiration() / 1000, cookieSecure);
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
