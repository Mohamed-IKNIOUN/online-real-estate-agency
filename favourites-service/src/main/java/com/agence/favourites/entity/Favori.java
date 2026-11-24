package com.agence.favourites.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Entite JPA representant une annonce favorite d'un client.
 */
@Entity
@Table(
        name = "favoris",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"client_id", "annonce_id"}
        )
)
public class Favori {

    /** Identifiant unique du favori. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifiant du client. */
    @Column(name = "client_id", nullable = false)
    private Long clientId;

    /** Identifiant de l'annonce. */
    @Column(name = "annonce_id", nullable = false)
    private Long annonceId;

    /**
     * Constructeur sans argument requis par JPA.
     */
    public Favori() {
    }

    /**
     * Construit un favori avec les identifiants du client et de l'annonce.
     *
     * @param identifiantClient identifiant du client
     * @param identifiantAnnonce identifiant de l'annonce
     */
    public Favori(
            final Long identifiantClient,
            final Long identifiantAnnonce) {
        this.clientId = identifiantClient;
        this.annonceId = identifiantAnnonce;
    }

    /**
     * Retourne l'identifiant du favori.
     *
     * @return identifiant du favori
     */
    public Long getId() {
        return id;
    }

    /**
     * Retourne l'identifiant du client.
     *
     * @return identifiant du client
     */
    public Long getClientId() {
        return clientId;
    }

    /**
     * Modifie l'identifiant du client.
     *
     * @param identifiantClient nouvel identifiant du client
     */
    public void setClientId(final Long identifiantClient) {
        this.clientId = identifiantClient;
    }

    /**
     * Retourne l'identifiant de l'annonce.
     *
     * @return identifiant de l'annonce
     */
    public Long getAnnonceId() {
        return annonceId;
    }

    /**
     * Modifie l'identifiant de l'annonce.
     *
     * @param identifiantAnnonce nouvel identifiant de l'annonce
     */
    public void setAnnonceId(final Long identifiantAnnonce) {
        this.annonceId = identifiantAnnonce;
    }
}
