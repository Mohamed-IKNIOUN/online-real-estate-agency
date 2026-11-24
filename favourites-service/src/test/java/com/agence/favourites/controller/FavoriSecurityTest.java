package com.agence.favourites.controller;

import com.agence.favourites.config.CorsConfig;
import com.agence.favourites.config.SecurityConfig;
import com.agence.favourites.entity.Favori;
import com.agence.favourites.service.FavoriService;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FavoriController.class)
@Import({SecurityConfig.class, CorsConfig.class})
class FavoriSecurityTest {

    private static final byte[] KEY = randomKey();

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private FavoriService service;

    @DynamicPropertySource
    static void configureKey(DynamicPropertyRegistry properties) {
        properties.add("favourites.security.jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "DELETE"})
    void rejectsMissingToken(String method) throws Exception {
        mvc.perform(favourites(method, 1)).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "DELETE"})
    void rejectsInvalidTokens(String method) throws Exception {
        Instant now = Instant.now();
        List<String> invalid = List.of(
                "malformed",
                token("1", "BUYER", now.minusSeconds(30), KEY),
                token("1", "BUYER", now.plusSeconds(300), randomKey()),
                token("not-a-number", "BUYER", now.plusSeconds(300), KEY),
                token("0", "BUYER", now.plusSeconds(300), KEY),
                token("9223372036854775808", "BUYER", now.plusSeconds(300), KEY),
                token("1", "BUYER", null, KEY));
        for (String bearer : invalid) {
            mvc.perform(favourites(method, 1).header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer))
                    .andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "DELETE"})
    void rejectsWrongRolesAndCrossClientAccess(String method) throws Exception {
        for (String role : List.of("AGENT", "ADMIN")) {
            mvc.perform(favourites(method, 1).header(HttpHeaders.AUTHORIZATION, bearer("1", role)))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(favourites(method, 2).header(HttpHeaders.AUTHORIZATION, bearer("1", "BUYER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void ownerCanListCreateAndDeleteFavourites() throws Exception {
        Favori favourite = new Favori(1L, 10L);
        when(service.consulter(1L)).thenReturn(List.of(favourite));
        when(service.ajouter(1L, 10L)).thenReturn(favourite);
        String bearer = bearer("1", "BUYER");

        mvc.perform(favourites("GET", 1).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clientId").value(1))
                .andExpect(jsonPath("$[0].annonceId").value(10));
        mvc.perform(favourites("POST", 1).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").value(1))
                .andExpect(jsonPath("$.annonceId").value(10));
        mvc.perform(favourites("DELETE", 1).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(service).consulter(1L);
        verify(service).ajouter(1L, 10L);
        verify(service).supprimer(1L, 10L);
    }

    @Test
    void allowsApprovedFrontendPreflightWithoutToken() throws Exception {
        String origin = "http://localhost:5173";
        mvc.perform(options("/api/favoris/1/10")
                        .header(HttpHeaders.ORIGIN, origin)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin));
        verifyNoInteractions(service);
    }

    private static MockHttpServletRequestBuilder favourites(String method, long clientId) {
        String path = "GET".equals(method) ? "/api/favoris/client/{id}" : "/api/favoris/{id}/10";
        return request(HttpMethod.valueOf(method), path, clientId);
    }

    private static String bearer(String subject, String role) throws Exception {
        return "Bearer " + token(subject, role, Instant.now().plusSeconds(300), KEY);
    }

    private static String token(String subject, String role, Instant expiry, byte[] key) throws Exception {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder().subject(subject).claim("role", role);
        if (expiry != null) {
            claims.expirationTime(Date.from(expiry));
        }
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims.build());
        jwt.sign(new MACSigner(key));
        return jwt.serialize();
    }

    private static byte[] randomKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return key;
    }
}
