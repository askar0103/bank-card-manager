package com.example.bankcards.dto.request;

import com.example.bankcards.validation.annotation.ValidCardNumber;
import com.example.bankcards.validation.annotation.ValidId;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record CardCreateRequest(

        @ValidCardNumber
        String cardNumber,

        @ValidId
        Long ownerId,

        @NotNull(message = "Expiry date is required")
        @Future(message = "Expiry date must be in the future")
        LocalDate expiryDate,

        @NotNull(message = "Initial balance is required")
        @DecimalMin(value = "0.00", message = "Initial balance cannot be negative")
        BigDecimal initialBalance
) {
}
