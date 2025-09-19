package com.example.bankcards.exception.badrequest;

public class TransferOperationNotAllowedException extends BadRequestException {

    public TransferOperationNotAllowedException(String message) {
        super(message);
    }
}
