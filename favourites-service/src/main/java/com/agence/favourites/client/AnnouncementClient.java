package com.agence.favourites.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Client REST permettant de communiquer avec le microservice Annonces.
 */
@Component
public class AnnouncementClient {

    /** Client HTTP utilise pour verifier les annonces. */
    private final RestClient restClient;

    /**
     * Initialise le client REST avec l'URL du service Annonces.
     *
     * @param baseUrl URL du microservice Annonces
     */
    public AnnouncementClient(
            @Value("${announcements.service.url}")
            final String baseUrl) {

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Verifie l'existence d'une annonce dans le microservice Annonces.
     *
     * @param annonceId identifiant de l'annonce a verifier
     * @throws ResponseStatusException si l'annonce est introuvable
     *         ou si le service Annonces est indisponible
     */
    public void verifierAnnonce(final Long annonceId) {

        try {
            restClient.get()
                    .uri("/{id}", annonceId)
                    .retrieve()
                    .toBodilessEntity();

        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Annonce introuvable"
            );

        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Service Annonces indisponible"
            );

        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Erreur du service Annonces"
            );
        }
    }
}
