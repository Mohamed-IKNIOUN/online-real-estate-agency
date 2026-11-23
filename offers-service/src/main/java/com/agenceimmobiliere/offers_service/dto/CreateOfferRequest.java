package com.agenceimmobiliere.offers_service.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class CreateOfferRequest {

	@NotBlank(message = "Message is required")
	@Size(max = 2000, message = "Message cannot exceed 2000 characters")
	private String message;

	@NotNull(message = "Proposed amount is required")
	@DecimalMin(value = "0.01", message = "Proposed amount must be greater than zero")
	private BigDecimal proposedAmount;

	@NotNull(message = "Announcement ID is required")
	@Positive(message = "Announcement ID must be positive")
	private Long announcementId;

	@NotNull(message = "Buyer ID is required")
	@Positive(message = "Buyer ID must be positive")
	private Long buyerId;

    public CreateOfferRequest() {
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public BigDecimal getProposedAmount() {
        return proposedAmount;
    }

    public void setProposedAmount(BigDecimal proposedAmount) {
        this.proposedAmount = proposedAmount;
    }

    public Long getAnnouncementId() {
        return announcementId;
    }

    public void setAnnouncementId(Long announcementId) {
        this.announcementId = announcementId;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }
}