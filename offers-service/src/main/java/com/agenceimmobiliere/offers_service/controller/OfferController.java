package com.agenceimmobiliere.offers_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.agenceimmobiliere.offers_service.entity.Offer;
import com.agenceimmobiliere.offers_service.service.OfferService;

@RestController
@RequestMapping("/api/offers")
public class OfferController {

    private final OfferService offerService;

    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    // GET /api/offers
    @GetMapping
    public List<Offer> getAllOffers() {
        return offerService.getAllOffers();
    }

    // GET /api/offers/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Offer> getOfferById(@PathVariable Long id) {

        return offerService.getOfferById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/offers
    @PostMapping
    public ResponseEntity<Offer> createOffer(@RequestBody Offer offer) {

        Offer createdOffer = offerService.createOffer(offer);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdOffer);
    }

    // PUT /api/offers/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Offer> updateOffer(
            @PathVariable Long id,
            @RequestBody Offer updatedOffer) {

        return offerService.updateOffer(id, updatedOffer)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/offers/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOffer(@PathVariable Long id) {

        boolean deleted = offerService.deleteOffer(id);

        if (!deleted) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    // GET /api/offers/buyer/{buyerId}
    @GetMapping("/buyer/{buyerId}")
    public List<Offer> getOffersByBuyerId(@PathVariable Long buyerId) {
        return offerService.getOffersByBuyerId(buyerId);
    }

    // GET /api/offers/announcement/{announcementId}
    @GetMapping("/announcement/{announcementId}")
    public List<Offer> getOffersByAnnouncementId(
            @PathVariable Long announcementId) {

        return offerService.getOffersByAnnouncementId(announcementId);
    }
}