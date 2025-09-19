package com.example.bankcards.dto.request;

import com.example.bankcards.validation.annotation.AtLeastOneNotNull;
import com.example.bankcards.validation.annotation.ValidUsername;

@AtLeastOneNotNull
public record UserUpdateRequest(

        @ValidUsername
        String newUsername
) {
}
