package com.agenceimmobiliere.offers_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.agenceimmobiliere.offers_service.enums.OfferStatus;

public class OfferResponse {

    private Long id;
    private String message;
    private BigDecimal proposedAmount;
    private OfferStatus status;
    private LocalDateTime submittedAt;
    private Long announcementId;
    private Long buyerId;

    public OfferResponse() {
    }

    public OfferResponse(
            Long id,
            String message,
            BigDecimal proposedAmount,
            OfferStatus status,
            LocalDateTime submittedAt,
            Long announcementId,
            Long buyerId) {

        this.id = id;
        this.message = message;
        this.proposedAmount = proposedAmount;
        this.status = status;
        this.submittedAt = submittedAt;
        this.announcementId = announcementId;
        this.buyerId = buyerId;
    }

    public Long getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public BigDecimal getProposedAmount() {
        return proposedAmount;
    }

    public OfferStatus getStatus() {
        return status;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public Long getAnnouncementId() {
        return announcementId;
    }

    public Long getBuyerId() {
        return buyerId;
    }
}