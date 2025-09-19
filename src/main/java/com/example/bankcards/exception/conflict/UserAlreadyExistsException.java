package com.example.bankcards.exception.conflict;

public class UserAlreadyExistsException extends ConflictException {

    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
