package com.agence.favourites.repository;

import com.agence.favourites.entity.Favori;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository JPA pour la gestion des favoris.
 *
 * Permet de rechercher et verifier les annonces favorites des clients.
 */
public interface FavoriRepository extends JpaRepository<Favori, Long> {

    /**
     * Recherche tous les favoris d'un client.
     *
     * @param clientId identifiant du client
     * @return liste des favoris du client
     */
    List<Favori> findByClientId(Long clientId);

    /**
     * Verifie si une annonce est deja dans les favoris d'un client.
     *
     * @param clientId identifiant du client
     * @param annonceId identifiant de l'annonce
     * @return true si le favori existe, false sinon
     */
    boolean existsByClientIdAndAnnonceId(
            Long clientId,
            Long annonceId
    );

    /**
     * Recherche un favori par client et annonce.
     *
     * @param clientId identifiant du client
     * @param annonceId identifiant de l'annonce
     * @return favori trouve ou Optional vide
     */
    Optional<Favori> findByClientIdAndAnnonceId(
            Long clientId,
            Long annonceId
    );
}
