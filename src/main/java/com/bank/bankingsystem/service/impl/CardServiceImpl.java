package com.bank.bankingsystem.service.impl;

import com.bank.bankingsystem.dto.response.CardResponse;
import com.bank.bankingsystem.exception.BusinessRuleException;
import com.bank.bankingsystem.exception.ResourceNotFoundException;
import com.bank.bankingsystem.exception.UnauthorizedAccountAccessException;
import com.bank.bankingsystem.model.Account;
import com.bank.bankingsystem.model.Card;
import com.bank.bankingsystem.model.Customer;
import com.bank.bankingsystem.repository.AccountRepository;
import com.bank.bankingsystem.repository.AdminAuditLogRepository;
import com.bank.bankingsystem.repository.CardRepository;
import com.bank.bankingsystem.repository.CustomerRepository;
import com.bank.bankingsystem.service.CardService;
import com.bank.bankingsystem.util.CurrentUserUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;

/**
 * EDUCATIONAL SIMULATOR ONLY.
 * Generated card numbers are Luhn-valid but NOT issued by any bank.
 * They will be REJECTED at any real payment terminal.
 */
@Service
public class CardServiceImpl implements CardService {

    private static final int MAX_CARDS_PER_CUSTOMER = 3;
    private static final SecureRandom RNG = new SecureRandom();

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final AdminAuditLogRepository auditRepository;
    private final PasswordEncoder passwordEncoder;

    public CardServiceImpl(CardRepository cardRepository,
                           AccountRepository accountRepository,
                           CustomerRepository customerRepository,
                           AdminAuditLogRepository auditRepository,
                           PasswordEncoder passwordEncoder) {
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.auditRepository = auditRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<CardResponse> listMyCards() {
        Customer me = currentCustomer();
        return cardRepository.findByCustomerId(me.getId()).stream()
                .map(CardResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public CardResponse generateCard(Long accountId) {
        Customer me = currentCustomer();
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        if (!account.getCustomerId().equals(me.getId())) {
            throw new UnauthorizedAccountAccessException("You do not own this account");
        }

        int existing = cardRepository.countByCustomerId(me.getId());
        if (existing >= MAX_CARDS_PER_CUSTOMER) {
            throw new BusinessRuleException("Card limit reached (" + MAX_CARDS_PER_CUSTOMER + ")");
        }

        String number = generateLuhnValidVisaNumber();
        String cvv = String.format("%03d", RNG.nextInt(1000));
        LocalDate expiry = LocalDate.now().plusYears(3);

        Card card = new Card();
        card.setCustomerId(me.getId());
        card.setAccountId(account.getId());
        card.setCardNumber(number);
        card.setLast4(number.substring(number.length() - 4));
        card.setBrand("VISA");
        card.setCardholderName((me.getFirstName() + " " + me.getLastName()).toUpperCase());
        card.setExpiryMonth(expiry.getMonthValue());
        card.setExpiryYear(expiry.getYear());
        card.setCvvHash(passwordEncoder.encode(cvv));

        Long id = cardRepository.insert(card);
        auditRepository.log(CurrentUserUtil.currentUserId(),
                "CARD_GENERATED", "CARD", id, "linked to account " + account.getAccountNumber());

        return CardResponse.from(cardRepository.findById(id).orElseThrow());
    }

    @Override
    @Transactional
    public CardResponse setStatus(Long cardId, String status) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
        Customer me = currentCustomer();
        if (!card.getCustomerId().equals(me.getId())) {
            throw new UnauthorizedAccountAccessException("You do not own this card");
        }
        String s = status == null ? "" : status.trim().toUpperCase();
        if (!List.of("ACTIVE", "FROZEN", "CANCELLED").contains(s)) {
            throw new BusinessRuleException("Invalid status. Allowed: ACTIVE, FROZEN, CANCELLED");
        }
        cardRepository.updateStatus(cardId, s);
        auditRepository.log(CurrentUserUtil.currentUserId(),
                "CARD_STATUS_CHANGE", "CARD", cardId, "set status to " + s);
        return CardResponse.from(cardRepository.findById(cardId).orElseThrow());
    }

    @Override
    public String revealCvv(Long cardId) {
        // In this educational build we cannot reverse BCrypt — so we generate a
        // fresh CVV each time "reveal" is called. Not real behaviour; a demo
        // convenience. A real system would store the CVV encrypted (not hashed)
        // and require re-authentication to reveal.
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
        Customer me = currentCustomer();
        if (!card.getCustomerId().equals(me.getId())) {
            throw new UnauthorizedAccountAccessException("You do not own this card");
        }
        return String.format("%03d", RNG.nextInt(1000));
    }

    // ---------------- helpers ----------------

    private Customer currentCustomer() {
        Long userId = CurrentUserUtil.currentUserId();
        return customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile missing"));
    }

    /** Generates a Visa-prefixed (4), 16-digit number that passes the Luhn check. */
    private String generateLuhnValidVisaNumber() {
        String candidate;
        int attempts = 0;
        do {
            StringBuilder sb = new StringBuilder("4");
            for (int i = 0; i < 14; i++) sb.append(RNG.nextInt(10));
            sb.append(luhnCheckDigit(sb.toString()));
            candidate = sb.toString();
            attempts++;
        } while (cardRepository.existsByNumber(candidate) && attempts < 20);
        return candidate;
    }

    private int luhnCheckDigit(String partial) {
        int sum = 0;
        boolean doubleDigit = true; // rightmost of partial gets doubled
        for (int i = partial.length() - 1; i >= 0; i--) {
            int d = partial.charAt(i) - '0';
            if (doubleDigit) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
            doubleDigit = !doubleDigit;
        }
        return (10 - (sum % 10)) % 10;
    }
}
