package com.agenceimmobiliere.offers_service.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.agenceimmobiliere.offers_service.entity.Offer;
import com.agenceimmobiliere.offers_service.repository.OfferRepository;

@Service
public class OfferService {

    private final OfferRepository offerRepository;

    public OfferService(OfferRepository offerRepository) {
        this.offerRepository = offerRepository;
    }

    // Get all offers
    public List<Offer> getAllOffers() {
        return offerRepository.findAll();
    }

    // Get one offer by ID
    public Optional<Offer> getOfferById(Long id) {
        return offerRepository.findById(id);
    }

    // Create a new offer
    public Offer createOffer(Offer offer) {
        return offerRepository.save(offer);
    }

    // Update an existing offer
    public Optional<Offer> updateOffer(Long id, Offer updatedOffer) {

        Optional<Offer> existingOffer = offerRepository.findById(id);

        if (existingOffer.isEmpty()) {
            return Optional.empty();
        }

        Offer offer = existingOffer.get();

        offer.setMessage(updatedOffer.getMessage());
        offer.setProposedAmount(updatedOffer.getProposedAmount());
        offer.setAnnouncementId(updatedOffer.getAnnouncementId());
        offer.setBuyerId(updatedOffer.getBuyerId());

        return Optional.of(offerRepository.save(offer));
    }

    // Delete an offer
    public boolean deleteOffer(Long id) {

        if (!offerRepository.existsById(id)) {
            return false;
        }

        offerRepository.deleteById(id);
        return true;
    }

    // Get offers by buyer ID
    public List<Offer> getOffersByBuyerId(Long buyerId) {
        return offerRepository.findByBuyerId(buyerId);
    }

    // Get offers by announcement ID
    public List<Offer> getOffersByAnnouncementId(Long announcementId) {
        return offerRepository.findByAnnouncementId(announcementId);
    }
}