package com.naro.auth_service.service;

import com.naro.auth_service.entity.RefreshToken;
import com.naro.auth_service.entity.User;
import com.naro.auth_service.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${application.security.jwt.refresh-expiration}")
    private Long refreshExpiration;

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public RefreshToken create(User user) {
        refreshTokenRepository.deleteAllByUser(user);
        return refreshTokenRepository.save(
            RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshExpiration))
                .build()
        );
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken verify(String tokenValue) {
        RefreshToken token = refreshTokenRepository.findByToken(tokenValue)
            .orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token invalido"));

        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expirado");
        }
        return token;
    }

    @Transactional
    public void deleteAllByUser(User user) {
        refreshTokenRepository.deleteAllByUser(user);
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void cleanExpiredOnStartup() {
        refreshTokenRepository.deleteExpired(Instant.now());
    }

}
