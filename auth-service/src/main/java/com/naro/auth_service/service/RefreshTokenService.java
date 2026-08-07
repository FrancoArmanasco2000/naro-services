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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
        String rawToken = UUID.randomUUID().toString();
        RefreshToken saved = refreshTokenRepository.save(
            RefreshToken.builder()
                .user(user)
                .token(hashToken(rawToken))
                .expiryDate(Instant.now().plusMillis(refreshExpiration))
                .build()
        );
        // Return a transient (unmanaged) copy exposing the RAW token to the
        // caller — e.g. for the response cookie — while only the hash is
        // ever persisted. This object is intentionally not the managed
        // entity, so mutating its `token` field can't cause Hibernate to
        // flush the raw value back over the stored hash.
        return RefreshToken.builder()
            .id(saved.getId())
            .user(saved.getUser())
            .token(rawToken)
            .expiryDate(saved.getExpiryDate())
            .build();
    }

    public Optional<RefreshToken> findByToken(String rawToken) {
        return refreshTokenRepository.findByToken(hashToken(rawToken));
    }

    public RefreshToken verify(String tokenValue) {
        RefreshToken token = refreshTokenRepository.findByToken(hashToken(tokenValue))
            .orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token invalido"));

        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expirado");
        }
        return token;
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
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
