package com.agenceimmobiliere.offers_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

//import com.agenceimmobiliere.offers_service.entity.Offer;
import com.agenceimmobiliere.offers_service.service.OfferService;

import com.agenceimmobiliere.offers_service.dto.CreateOfferRequest;
import com.agenceimmobiliere.offers_service.dto.UpdateOfferRequest;
import com.agenceimmobiliere.offers_service.dto.OfferResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/offers")
public class OfferController {

    private final OfferService offerService;

    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    // GET /api/offers
    @GetMapping
    public List<OfferResponse> getAllOffers() {
        return offerService.getAllOffers();
    }

    // GET /api/offers/{id}
    @GetMapping("/{id}")
    public ResponseEntity<OfferResponse> getOfferById(@PathVariable Long id) {

        return offerService.getOfferById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

 // POST /api/offers
    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<OfferResponse> createOffer(
            @Valid @RequestBody CreateOfferRequest request,
            Authentication authentication) {

        Long buyerId = (Long) authentication.getPrincipal();

        OfferResponse createdOffer =
                offerService.createOffer(request, buyerId);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdOffer);
    }

 // PUT /api/offers/{id}
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<OfferResponse> updateOffer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOfferRequest request,
            Authentication authentication) {

        Long buyerId = (Long) authentication.getPrincipal();

        return offerService.updateOffer(id, request, buyerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/offers/{id}
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Void> deleteOffer(
            @PathVariable Long id,
            Authentication authentication) {

        Long buyerId = (Long) authentication.getPrincipal();

        boolean deleted = offerService.deleteOffer(id, buyerId);

        if (!deleted) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    // GET /api/offers/buyer/{buyerId}
    @GetMapping("/buyer/{buyerId}")
    public List<OfferResponse> getOffersByBuyerId(@PathVariable Long buyerId) {
        return offerService.getOffersByBuyerId(buyerId);
    }

    // GET /api/offers/announcement/{announcementId}
    @GetMapping("/announcement/{announcementId}")
    public List<OfferResponse> getOffersByAnnouncementId(
            @PathVariable Long announcementId) {

        return offerService.getOffersByAnnouncementId(announcementId);
    }
}