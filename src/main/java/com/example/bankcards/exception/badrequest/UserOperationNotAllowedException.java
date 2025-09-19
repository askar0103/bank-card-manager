package com.example.bankcards.exception.badrequest;

public class UserOperationNotAllowedException extends BadRequestException {

    public UserOperationNotAllowedException(String message) {
        super(message);
    }
}
