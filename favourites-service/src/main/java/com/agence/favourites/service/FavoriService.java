package com.agence.favourites.service;

import com.agence.favourites.client.AnnouncementClient;
import com.agence.favourites.entity.Favori;
import com.agence.favourites.repository.FavoriRepository;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service de gestion des annonces favorites des clients.
 */
@Service
public class FavoriService {

    /** Repository pour la gestion des favoris. */
    private final FavoriRepository repository;

    /** Client du microservice des annonces. */
    private final AnnouncementClient announcementClient;

    /**
     * Initialise le service des favoris.
     *
     * @param favoriRepository repository des favoris
     * @param clientAnnonces client du microservice annonces
     */
    public FavoriService(
            final FavoriRepository favoriRepository,
            final AnnouncementClient clientAnnonces) {
        this.repository = favoriRepository;
        this.announcementClient = clientAnnonces;
    }

    /**
     * Ajoute une annonce aux favoris d'un client.
     *
     * @param clientId identifiant du client
     * @param annonceId identifiant de l'annonce
     * @return favori enregistre
     */
    public Favori ajouter(
            final Long clientId,
            final Long annonceId) {

        if (clientId == null
                || clientId <= 0
                || annonceId == null
                || annonceId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Identifiants invalides"
            );
        }

        if (repository.existsByClientIdAndAnnonceId(
                clientId, annonceId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cette annonce est deja dans les favoris"
            );
        }

        // Verification aupres du microservice Annonces
        announcementClient.verifierAnnonce(annonceId);

        return repository.save(
                new Favori(clientId, annonceId)
        );
    }

    /**
     * Consulte les favoris d'un client.
     *
     * @param clientId identifiant du client
     * @return liste des favoris du client
     */
    public List<Favori> consulter(final Long clientId) {
        if (clientId == null || clientId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Identifiant client invalide"
            );
        }

        return repository.findByClientId(clientId);
    }

    /**
     * Supprime une annonce des favoris d'un client.
     *
     * @param clientId identifiant du client
     * @param annonceId identifiant de l'annonce
     */
    public void supprimer(
            final Long clientId,
            final Long annonceId) {

        if (clientId == null
                || clientId <= 0
                || annonceId == null
                || annonceId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Identifiants invalides"
            );
        }

        final Favori favori = repository
                .findByClientIdAndAnnonceId(clientId, annonceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Favori introuvable"
                ));

        repository.delete(favori);
    }
}
