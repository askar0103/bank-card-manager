package com.example.bankcards.validator;

import com.example.bankcards.entity.Card;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.exception.badrequest.TransferOperationNotAllowedException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TransferValidator {

    public void validateForTransfer(Card fromCard, Card toCard, BigDecimal amount) {
        if (fromCard.getId().equals(toCard.getId())) {
            throw new TransferOperationNotAllowedException("Cannot transfer to the same card");
        }

        if (fromCard.getCardStatus() != CardStatus.ACTIVE) {
            throw new TransferOperationNotAllowedException(
                    "Cannot transfer from card with status: %s".formatted(fromCard.getCardStatus())
            );
        }

        if (toCard.getCardStatus() != CardStatus.ACTIVE) {
            throw new TransferOperationNotAllowedException(
                    "Cannot transfer to card with status: %s".formatted(toCard.getCardStatus())
            );
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransferOperationNotAllowedException("Transfer amount must be positive");
        }

        if (fromCard.getBalance().compareTo(amount) < 0) {
            throw new TransferOperationNotAllowedException(
                    "Insufficient balance for transfer. Available: %s, Required: %s"
                            .formatted(fromCard.getBalance(), amount)
            );
        }
    }
}
