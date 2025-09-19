package com.example.bankcards.dto.response;

import com.example.bankcards.enums.Role;

public record UserAuthResponse(
        Long id,
        String username,
        String passwordHash,
        boolean enabled,
        Role role
) {
}
