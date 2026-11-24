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

import org.springframework.security.access.AccessDeniedException;

//import org.springframework.security.core.Authentication;
//import org.springframework.web.server.ResponseStatusException;
//import org.springframework.http.HttpStatus;
//import org.springframework.security.access.AccessDeniedException;

import com.agenceimmobiliere.offers_service.client.AnnouncementClient;
import com.agenceimmobiliere.offers_service.dto.AnnouncementResponse;

@RestController
@RequestMapping("/api/offers")
public class OfferController {

    private final OfferService offerService;
    private final AnnouncementClient announcementClient;

    public OfferController(OfferService offerService, AnnouncementClient announcementClient) {
        this.offerService = offerService;
        this.announcementClient = announcementClient;
    }

    // GET /api/offers
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<OfferResponse> getAllOffers() {
        return offerService.getAllOffers();
    }

    // GET /api/offers/{id}
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    public ResponseEntity<OfferResponse> getOfferById(
            @PathVariable Long id,
            Authentication authentication) {

        return offerService.getOfferById(id)
                .map(offer -> {
                    Long userId = (Long) authentication.getPrincipal();

                    boolean isAdmin = authentication.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

                    if (!isAdmin && !offer.getBuyerId().equals(userId)) {
                        throw new AccessDeniedException(
                                "You cannot view another buyer's offer.");
                    }

                    return ResponseEntity.ok(offer);
                })
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
    @PreAuthorize("hasRole('BUYER')")
    public List<OfferResponse> getOffersByBuyerId(
            @PathVariable Long buyerId,
            Authentication authentication) {

        Long currentBuyerId = (Long) authentication.getPrincipal();

        if (!currentBuyerId.equals(buyerId)) {
            throw new AccessDeniedException("You cannot view another buyer's offers.");
        }

        return offerService.getOffersByBuyerId(currentBuyerId);
    }

    // GET /api/offers/announcement/{announcementId}
    @GetMapping("/announcement/{announcementId}")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public List<OfferResponse> getOffersByAnnouncementId(
            @PathVariable Long announcementId,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            AnnouncementResponse announcement =
                    announcementClient.getAnnouncement(announcementId);

            if (!userId.equals(announcement.getAgentId())) {
                throw new AccessDeniedException(
                        "You cannot view offers for another agent's announcement.");
            }
        }

        return offerService.getOffersByAnnouncementId(announcementId);
    }
}