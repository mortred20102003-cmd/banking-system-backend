package com.bank.bankingsystem.dto.response;

import com.bank.bankingsystem.model.Card;

public class CardResponse {
    private Long id;
    private Long accountId;
    private String maskedNumber;      // **** **** **** 1234
    private String last4;
    private String brand;
    private String cardholderName;
    private String expiry;            // MM/YY
    private String status;
    private String createdAt;

    public static CardResponse from(Card c) {
        CardResponse r = new CardResponse();
        r.id = c.getId();
        r.accountId = c.getAccountId();
        r.last4 = c.getLast4();
        r.maskedNumber = "**** **** **** " + c.getLast4();
        r.brand = c.getBrand();
        r.cardholderName = c.getCardholderName();
        r.expiry = String.format("%02d/%02d", c.getExpiryMonth(), c.getExpiryYear() % 100);
        r.status = c.getStatus();
        r.createdAt = c.getCreatedAt() == null ? null : c.getCreatedAt().toString();
        return r;
    }

    public Long getId() { return id; }
    public Long getAccountId() { return accountId; }
    public String getMaskedNumber() { return maskedNumber; }
    public String getLast4() { return last4; }
    public String getBrand() { return brand; }
    public String getCardholderName() { return cardholderName; }
    public String getExpiry() { return expiry; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
}
