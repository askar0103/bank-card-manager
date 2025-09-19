package com.example.bankcards.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserPasswordUpdateRequest(

        @NotNull(message = "New password is required")
        @Size(min = 4, max = 20)
        String newPassword
) {
}
