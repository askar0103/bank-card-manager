package com.example.bankcards.dto.response;

import com.example.bankcards.enums.Role;

public record LoginResponse(
        String username,
        Role role,
        String token
) {
}
