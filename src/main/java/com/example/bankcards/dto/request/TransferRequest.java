package com.example.bankcards.dto.request;

import com.example.bankcards.validation.annotation.ValidId;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferRequest(

        @ValidId
        Long fromCardId,

        @ValidId
        Long toCardId,

        @NotNull(message = "Amount is required")
        @Positive(message = "Transfer amount must be positive")
        @Digits(integer = 8, fraction = 2)
        BigDecimal amount
) {
}
