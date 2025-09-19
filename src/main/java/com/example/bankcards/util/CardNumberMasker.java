package com.example.bankcards.util;

public class CardNumberMasker {

    public static String mask(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return cardNumber;
        }
        return "**** **** **** %s".formatted(
                cardNumber.substring(cardNumber.length() - 4)
        );
    }
}
