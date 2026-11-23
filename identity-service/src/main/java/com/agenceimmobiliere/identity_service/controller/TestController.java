package com.agenceimmobiliere.identity_service.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/test/me")
    public String getCurrentUser(Authentication authentication) {

        return "Authenticated user ID: " + authentication.getPrincipal()
                + " | Authorities: " + authentication.getAuthorities();
    }
}