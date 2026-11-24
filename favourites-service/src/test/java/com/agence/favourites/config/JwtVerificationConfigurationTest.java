package com.agence.favourites.config;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.assertj.core.api.Assertions.assertThat;

class JwtVerificationConfigurationTest {

    private static final String KEY_VARIABLE = "FAVOURITES_JWT_SECRET";

    @Test
    void startupFailsWhenExternalKeyIsMissing() {
        startupWith(Map.of()).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining(KEY_VARIABLE);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not valid Base64", "dGVzdA=="})
    void startupRejectsEmptyMalformedAndShortKeys(String configuredKey) {
        startupWith(Map.of(KEY_VARIABLE, configuredKey)).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    void startupAcceptsExternalHs256Key() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        startupWith(Map.of(KEY_VARIABLE, Base64.getEncoder().encodeToString(key)))
                .run(context -> assertThat(context).hasSingleBean(JwtDecoder.class));
    }

    private ApplicationContextRunner startupWith(Map<String, Object> environment) {
        return new ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().getPropertySources()
                        .replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                                new MapPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                                        environment)))
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(DecoderStartup.class);
    }

    @Configuration(proxyBeanMethods = false)
    static class DecoderStartup {

        @Bean
        static PropertySourcesPlaceholderConfigurer placeholders() {
            return new PropertySourcesPlaceholderConfigurer();
        }

        @Bean
        JwtDecoder jwtDecoder(@Value("${favourites.security.jwt-secret}") String configuredKey) {
            return new SecurityConfig().favouritesJwtDecoder(configuredKey);
        }
    }
}
