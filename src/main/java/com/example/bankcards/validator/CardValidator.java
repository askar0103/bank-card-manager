package com.example.bankcards.validator;

import com.example.bankcards.entity.Card;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.enums.Role;
import com.example.bankcards.exception.badrequest.CardDataNotValidException;
import com.example.bankcards.exception.badrequest.CardOperationNotAllowedException;
import com.example.bankcards.exception.conflict.CardAlreadyExistsException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.util.CardNumberMasker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class CardValidator {

    private final CardRepository cardRepository;

    public void validateForCreate(Card card) {
        if (card.getOwner().getRole() == Role.ADMIN) {
            throw new CardOperationNotAllowedException("Cannot create a card for ADMIN user");
        }

        if (!card.getExpiryDate().isAfter(LocalDate.now())) {
            throw new CardDataNotValidException("Expiration date must be in the future");
        }

        if (cardRepository.existsByCardNumberHash(card.getCardNumberHash())) {
            String maskedCardNumber = CardNumberMasker.mask(card.getCardNumber());
            throw new CardAlreadyExistsException(
                    "Card with number %s already exists".formatted(maskedCardNumber)
            );
        }
    }

    public void validateForBlock(Card card) {
        if (card.getCardStatus() != CardStatus.ACTIVE) {
            throw new CardOperationNotAllowedException(
                    "Cannot block card with status: %s".formatted(card.getCardStatus())
            );
        }
    }

    public void validateForActivate(Card card) {
        if (card.getCardStatus() != CardStatus.BLOCKED) {
            throw new CardOperationNotAllowedException(
                    "Cannot activate card with status: %s".formatted(card.getCardStatus())
            );
        }
    }

    public void validateForDelete(Card card) {
        if (card.getCardStatus() == CardStatus.ACTIVE) {
            throw new CardOperationNotAllowedException(
                    "Cannot delete card with status: %s. Card must be blocked first"
                            .formatted(card.getCardStatus())
            );
        }

        if (card.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new CardOperationNotAllowedException(
                    "Cannot delete card with non-zero balance: %s".formatted(card.getBalance())
            );
        }
    }
}
