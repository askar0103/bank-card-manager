package com.example.bankcards.service.domain;

import com.example.bankcards.entity.Card;
import com.example.bankcards.validator.TransferValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransferDomainService {

    private final TransferValidator transferValidator;

    public void transfer(Card fromCard, Card toCard, BigDecimal amount) {
        transferValidator.validateForTransfer(fromCard, toCard, amount);
        fromCard.setBalance(fromCard.getBalance().subtract(amount));
        toCard.setBalance(toCard.getBalance().add(amount));
    }
}
