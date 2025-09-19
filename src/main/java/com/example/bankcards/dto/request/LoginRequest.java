package com.example.bankcards.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "Username cannot be blank")
        @Size(min = 2, max = 20)
        String username,

        @NotBlank(message = "Password cannot be blank")
        @Size(min = 3, max = 20)
        String password
) {
}
