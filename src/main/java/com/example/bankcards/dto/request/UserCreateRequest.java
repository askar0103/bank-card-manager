package com.example.bankcards.dto.request;

import com.example.bankcards.enums.Role;
import com.example.bankcards.validation.annotation.ValidUsername;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(

        @ValidUsername
        String username,

        @NotBlank(message = "Password cannot be blank")
        @Size(min = 3, max = 20)
        String password,

        @NotNull(message = "Role is required")
        Role role
) {
}
