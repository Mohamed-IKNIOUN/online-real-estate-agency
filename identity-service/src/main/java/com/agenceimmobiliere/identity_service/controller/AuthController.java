package com.agenceimmobiliere.identity_service.controller;

import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.agenceimmobiliere.identity_service.dto.RegisterRequest;
import com.agenceimmobiliere.identity_service.dto.RegisterResponse;
import com.agenceimmobiliere.identity_service.entity.RefreshToken;
import com.agenceimmobiliere.identity_service.service.RefreshTokenService;
import com.agenceimmobiliere.identity_service.service.UserService;

import com.agenceimmobiliere.identity_service.dto.LoginRequest;
import com.agenceimmobiliere.identity_service.dto.LoginResponse;

import jakarta.validation.Valid;

import com.agenceimmobiliere.identity_service.dto.RefreshRequest;
import com.agenceimmobiliere.identity_service.dto.RefreshResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    private final RefreshTokenService refreshTokenService;

    public AuthController(
            UserService userService,
            RefreshTokenService refreshTokenService) {

        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        RegisterResponse response = userService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
    
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response = userService.login(request);

        return ResponseEntity.ok(response);
    }
    
    
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(
            @Valid @RequestBody RefreshRequest request) {

        RefreshResponse response = refreshTokenService.refresh(
                request.getRefreshToken()
        );

        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {

        RefreshToken refreshToken =
                refreshTokenService.validateRefreshToken(request.getRefreshToken());

        refreshTokenService.revokeToken(refreshToken);

        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(Authentication authentication) {

    	Long userId = (Long) authentication.getPrincipal();

        refreshTokenService.revokeAllUserTokens(userId);

        return ResponseEntity.noContent().build();
    }
    
    
    
}