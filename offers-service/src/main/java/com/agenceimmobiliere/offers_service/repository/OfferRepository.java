package com.agenceimmobiliere.offers_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.agenceimmobiliere.offers_service.entity.Offer;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    List<Offer> findByBuyerId(Long buyerId);

    List<Offer> findByAnnouncementId(Long announcementId);

}