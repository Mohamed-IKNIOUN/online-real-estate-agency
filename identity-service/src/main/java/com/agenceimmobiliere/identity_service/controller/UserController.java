package com.agenceimmobiliere.identity_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agenceimmobiliere.identity_service.dto.UserResponse;
import com.agenceimmobiliere.identity_service.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        return ResponseEntity.ok(userService.getCurrentUser(userId));
    }
}