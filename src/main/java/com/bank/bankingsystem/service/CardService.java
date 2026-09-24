package com.bank.bankingsystem.service;

import com.bank.bankingsystem.dto.response.CardResponse;

import java.util.List;

public interface CardService {
    List<CardResponse> listMyCards();
    CardResponse generateCard(Long accountId);
    CardResponse setStatus(Long cardId, String status);   // ACTIVE | FROZEN | CANCELLED
    String revealCvv(Long cardId);                        // returns plaintext CVV
}
