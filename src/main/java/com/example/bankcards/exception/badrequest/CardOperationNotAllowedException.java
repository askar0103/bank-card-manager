package com.example.bankcards.exception.badrequest;

public class CardOperationNotAllowedException extends BadRequestException {

    public CardOperationNotAllowedException(String message) {
        super(message);
    }
}

