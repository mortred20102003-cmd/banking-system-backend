package com.bank.bankingsystem.controller;

import com.bank.bankingsystem.dto.request.GenerateCardRequest;
import com.bank.bankingsystem.dto.response.ApiResponse;
import com.bank.bankingsystem.dto.response.CardResponse;
import com.bank.bankingsystem.service.CardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) { this.cardService = cardService; }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CardResponse>>> myCards() {
        return ResponseEntity.ok(ApiResponse.success("Cards retrieved", cardService.listMyCards()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CardResponse>> generate(@Valid @RequestBody GenerateCardRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Card generated", cardService.generateCard(req.getAccountId())));
    }

    @PatchMapping("/{cardId}/status")
    public ResponseEntity<ApiResponse<CardResponse>> setStatus(
            @PathVariable Long cardId, @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.success("Card status updated",
                cardService.setStatus(cardId, status)));
    }

    @PostMapping("/{cardId}/reveal-cvv")
    public ResponseEntity<ApiResponse<Map<String, String>>> revealCvv(@PathVariable Long cardId) {
        return ResponseEntity.ok(ApiResponse.success("CVV revealed",
                Map.of("cvv", cardService.revealCvv(cardId))));
    }
}
