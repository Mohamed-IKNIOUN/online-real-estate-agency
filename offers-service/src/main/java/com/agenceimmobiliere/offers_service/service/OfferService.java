package com.agenceimmobiliere.offers_service.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.agenceimmobiliere.offers_service.entity.Offer;
import com.agenceimmobiliere.offers_service.repository.OfferRepository;

import com.agenceimmobiliere.offers_service.dto.CreateOfferRequest;
import com.agenceimmobiliere.offers_service.dto.UpdateOfferRequest;
import com.agenceimmobiliere.offers_service.dto.OfferResponse;

@Service
public class OfferService {

    private final OfferRepository offerRepository;

    public OfferService(OfferRepository offerRepository) {
        this.offerRepository = offerRepository;
    }

    // Get all offers
    public List<OfferResponse> getAllOffers() {

        return offerRepository.findAll()
                .stream()
                .map(this::toOfferResponse)
                .toList();
    }

    // Get one offer by ID
    public Optional<OfferResponse> getOfferById(Long id) {

        return offerRepository.findById(id)
                .map(this::toOfferResponse);
    }

    // Create a new offer
    public OfferResponse createOffer(CreateOfferRequest request) {

        Offer offer = new Offer();

        offer.setMessage(request.getMessage());
        offer.setProposedAmount(request.getProposedAmount());
        offer.setAnnouncementId(request.getAnnouncementId());
        offer.setBuyerId(request.getBuyerId());

        Offer savedOffer = offerRepository.save(offer);

        return toOfferResponse(savedOffer);
    }

    // Update an existing offer
    public Optional<OfferResponse> updateOffer(
            Long id,
            UpdateOfferRequest request) {

        Optional<Offer> existingOffer = offerRepository.findById(id);

        if (existingOffer.isEmpty()) {
            return Optional.empty();
        }

        Offer offer = existingOffer.get();

        offer.setMessage(request.getMessage());
        offer.setProposedAmount(request.getProposedAmount());

        Offer updatedOffer = offerRepository.save(offer);

        return Optional.of(toOfferResponse(updatedOffer));
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
    public List<OfferResponse> getOffersByBuyerId(Long buyerId) {

        return offerRepository.findByBuyerId(buyerId)
                .stream()
                .map(this::toOfferResponse)
                .toList();
    }

    // Get offers by announcement ID
    public List<OfferResponse> getOffersByAnnouncementId(Long announcementId) {

        return offerRepository.findByAnnouncementId(announcementId)
                .stream()
                .map(this::toOfferResponse)
                .toList();
    }
    
    
    
    // Convert Offer entity into OfferResponse DTO
    private OfferResponse toOfferResponse(Offer offer) {

        return new OfferResponse(
                offer.getId(),
                offer.getMessage(),
                offer.getProposedAmount(),
                offer.getStatus(),
                offer.getSubmittedAt(),
                offer.getAnnouncementId(),
                offer.getBuyerId()
        );
    }
    
    
    
    
    
    
}