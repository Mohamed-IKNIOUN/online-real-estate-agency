package com.agenceimmobiliere.identity_service.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agenceimmobiliere.identity_service.dto.RefreshResponse;
import com.agenceimmobiliere.identity_service.entity.RefreshToken;
import com.agenceimmobiliere.identity_service.entity.User;
import com.agenceimmobiliere.identity_service.repository.RefreshTokenRepository;

import com.agenceimmobiliere.identity_service.security.JwtService;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long expirationDays;

    private final JwtService jwtService;
    
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            @Value("${jwt.refresh-expiration-days:7}") long expirationDays) {

        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.expirationDays = expirationDays;
    }

    @Transactional
    public String createRefreshToken(User user) {

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(
                LocalDateTime.now().plusDays(expirationDays)
        );
        refreshToken.setRevoked(false);

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    public RefreshToken validateRefreshToken(String rawToken) {

        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new IllegalArgumentException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Refresh token has expired");
        }

        return refreshToken;
    }

    @Transactional
    public void revokeToken(RefreshToken refreshToken) {
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    private String hashToken(String token) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getEncoder().encodeToString(hash);

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not hash refresh token", exception
            );
        }
    }
    
    
    
    
    @Transactional
    public RefreshResponse refresh(String rawToken) {

        RefreshToken oldToken = validateRefreshToken(rawToken);

        User user = oldToken.getUser();

        revokeToken(oldToken);

        String newRefreshToken = createRefreshToken(user);
        String newAccessToken = jwtService.generateToken(user);

        return new RefreshResponse(newAccessToken, newRefreshToken);
    }
    
    
}