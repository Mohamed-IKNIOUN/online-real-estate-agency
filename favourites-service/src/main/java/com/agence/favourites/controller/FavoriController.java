package com.agence.favourites.controller;

import com.agence.favourites.entity.Favori;
import com.agence.favourites.service.FavoriService;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controleur REST pour la gestion des favoris des clients.
 */
@RestController
@RequestMapping("/api/favoris")
public class FavoriController {

    /** Service de gestion des favoris. */
    private final FavoriService service;

    /**
     * Initialise le controleur des favoris.
     *
     * @param favoriService service de gestion des favoris
     */
    public FavoriController(final FavoriService favoriService) {
        this.service = favoriService;
    }

    /**
     * Ajoute une annonce aux favoris d'un client.
     *
     * @param principal identite authentifiee
     * @param clientId identifiant du client
     * @param annonceId identifiant de l'annonce
     * @return favori cree
     */
    @PostMapping("/{clientId}/{annonceId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Favori ajouter(
            final Principal principal,
            @PathVariable final Long clientId,
            @PathVariable final Long annonceId) {

        return service.ajouter(requireOwner(principal, clientId), annonceId);
    }

    /**
     * Consulte la liste des favoris d'un client.
     *
     * @param principal identite authentifiee
     * @param clientId identifiant du client
     * @return liste des favoris
     */
    @GetMapping("/client/{clientId}")
    public List<Favori> consulter(
            final Principal principal,
            @PathVariable final Long clientId) {

        return service.consulter(requireOwner(principal, clientId));
    }

    /**
     * Supprime une annonce des favoris d'un client.
     *
     * @param principal identite authentifiee
     * @param clientId identifiant du client
     * @param annonceId identifiant de l'annonce
     */
    @DeleteMapping("/{clientId}/{annonceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(
            final Principal principal,
            @PathVariable final Long clientId,
            @PathVariable final Long annonceId) {

        service.supprimer(requireOwner(principal, clientId), annonceId);
    }

    private static long requireOwner(
            final Principal principal, final long clientId) {
        if (clientId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Identifiant client invalide");
        }
        final long ownerId = Long.parseLong(principal.getName());
        if (ownerId != clientId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return ownerId;
    }
}
