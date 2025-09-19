package com.example.bankcards.exception.forbidden;

import org.springframework.security.access.AccessDeniedException;

public class CardAccessDeniedException extends AccessDeniedException {

    public CardAccessDeniedException(String message) {
        super(message);
    }
}
