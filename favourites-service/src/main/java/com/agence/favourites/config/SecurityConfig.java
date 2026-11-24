package com.agence.favourites.config;

import jakarta.servlet.DispatcherType;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

/** Configures bearer authentication for favourites. */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    /** Key size in bytes for the identity HS256 signing contract. */
    private static final int HS256_KEY_BYTES = 32;

    /**
     * Requires the buyer role and reuses MVC CORS rules.
     *
     * @param http HTTP security builder
     * @return security filter chain
     * @throws Exception if the security rules cannot be built
     */
    @Bean
    public SecurityFilterChain favouritesSecurity(
            final HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()
                        .requestMatchers("/api/favoris", "/api/favoris/**")
                        .hasRole("BUYER")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(server -> server.jwt(jwt -> jwt
                        .jwtAuthenticationConverter(
                                SecurityConfig::buyerToken)))
                .build();
    }

    /**
     * Validates identity-service access tokens using HS256.
     *
     * @param encodedKey externally configured Base64-encoded key
     * @return JWT decoder
     */
    @Bean
    public JwtDecoder favouritesJwtDecoder(
            @Value("${favourites.security.jwt-secret}")
            final String encodedKey) {
        final byte[] key;
        try {
            key = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid Base64 JWT verification key");
        }
        if (key.length != HS256_KEY_BYTES) {
            throw new IllegalArgumentException(
                    "JWT verification requires a 256-bit HS256 key");
        }

        final NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(key, "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(Duration.ZERO),
                SecurityConfig::validateIdentity));
        return decoder;
    }

    private static JwtAuthenticationToken buyerToken(final Jwt token) {
        final List<SimpleGrantedAuthority> authorities =
                "BUYER".equals(token.getClaims().get("role"))
                        ? List.of(new SimpleGrantedAuthority("ROLE_BUYER"))
                        : List.of();
        return new JwtAuthenticationToken(token, authorities);
    }

    private static OAuth2TokenValidatorResult validateIdentity(
            final Jwt token) {
        final String subject = token.getSubject();
        if (token.getExpiresAt() != null && subject != null
                && subject.matches("[1-9][0-9]*")) {
            try {
                Long.parseLong(subject);
                return OAuth2TokenValidatorResult.success();
            } catch (NumberFormatException exception) {
                // Subjects outside the Long range are invalid.
            }
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                "invalid_token", "Invalid JWT subject or expiry", null));
    }
}
