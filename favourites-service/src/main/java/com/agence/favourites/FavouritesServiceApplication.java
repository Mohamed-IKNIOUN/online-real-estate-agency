
package com.agence.favourites;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principale du microservice de gestion des favoris.
 *
 * Permet de demarrer l'application Spring Boot.
 */
@SpringBootApplication(proxyBeanMethods = false)
public final class FavouritesServiceApplication {

    /**
     * Constructeur prive pour eviter l'instanciation directe.
     */
    private FavouritesServiceApplication() {
    }

    /**
     * Point d'entree de l'application Spring Boot.
     *
     * @param args arguments de lancement
     */
    public static void main(final String[] args) {
        SpringApplication.run(FavouritesServiceApplication.class, args);
    }
}
