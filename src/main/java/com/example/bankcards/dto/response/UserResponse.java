package com.example.bankcards.dto.response;

import com.example.bankcards.enums.Role;

public record UserResponse(
        Long id,
        String username,
        Role role
) {
}
