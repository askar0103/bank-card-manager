package com.example.bankcards.exception.conflict;

public class CardAlreadyExistsException extends ConflictException {

    public CardAlreadyExistsException(String message) {
        super(message);
    }
}
