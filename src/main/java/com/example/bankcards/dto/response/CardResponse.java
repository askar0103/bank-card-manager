package com.example.bankcards.dto.response;

import com.example.bankcards.enums.CardStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CardResponse(
        Long id,
        String maskedCardNumber,
        LocalDate expiryDate,
        CardStatus cardStatus,
        BigDecimal balance
) {
}
