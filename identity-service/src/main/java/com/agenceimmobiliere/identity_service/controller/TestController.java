package com.agenceimmobiliere.identity_service.controller;

import org.springframework.security.access.prepost.PreAuthorize;
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

    @PreAuthorize("hasRole('BUYER')")
    @GetMapping("/api/test/buyer")
    public String buyerEndpoint() {
        return "Welcome BUYER! You can access buyer resources.";
    }

    @PreAuthorize("hasRole('AGENT')")
    @GetMapping("/api/test/agent")
    public String agentEndpoint() {
        return "Welcome AGENT! You can access agent resources.";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/test/admin")
    public String adminEndpoint() {
        return "Welcome ADMIN! You can access administrator resources.";
    }
}