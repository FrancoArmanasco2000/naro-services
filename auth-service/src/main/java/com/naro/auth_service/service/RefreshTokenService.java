package com.naro.auth_service.service;

import com.naro.auth_service.entity.RefreshToken;
import com.naro.auth_service.entity.User;
import com.naro.auth_service.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
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
        refreshTokenRepository.revokeAllByUser(user);
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
            .orElseThrow( () ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token invalido")
                );

        if(token.isRevoked() || token.getExpiryDate().isBefore(Instant.now())) {
            if(!token.isRevoked()) {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expirado ou revogado");
        }
        return token;
    }

    @Transactional
    public void revokeAllByUser(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanExpiredTokens() {
        refreshTokenRepository.deleteExpiredAndRevoked(Instant.now());
    }

}
