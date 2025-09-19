package com.example.bankcards.exception.badrequest;

public class CardDataNotValidException extends BadRequestException {

    public CardDataNotValidException(String message) {
        super(message);
    }
}
