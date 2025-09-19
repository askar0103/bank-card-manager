package com.example.bankcards.util;

import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

public class CardNumberEncryptor {

    private final TextEncryptor encryptor;

    public CardNumberEncryptor(String password, String salt) {
        password = HexFormat.of().formatHex(password.getBytes(StandardCharsets.UTF_8));
        salt = HexFormat.of().formatHex(salt.getBytes(StandardCharsets.UTF_8));

        this.encryptor = Encryptors.delux(password, salt);
    }

    public String encrypt(String rawCardNumber) {
        return encryptor.encrypt(rawCardNumber);
    }

    public String decrypt(String encryptedCardNumber) {
        return encryptor.decrypt(encryptedCardNumber);
    }
}
