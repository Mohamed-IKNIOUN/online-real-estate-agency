package com.agence.favourites.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration CORS du microservice favoris.
 *
 * Autorise les requetes provenant des applications frontend locales.
 */
@Configuration
public class CorsConfig {

    /**
     * Configure les autorisations CORS pour les endpoints API.
     *
     * @return configuration MVC contenant les regles CORS
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {

            /**
             * Definit les mappings CORS autorises.
             *
             * @param registry registre des configurations CORS
             */
            @Override
            public void addCorsMappings(final CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(
                                "http://localhost:3000",
                                "http://localhost:5173"
                        )
                        .allowedMethods(
                                "GET", "POST", "PUT",
                                "DELETE", "OPTIONS"
                        )
                        .allowedHeaders("*");
            }
        };
    }
}
