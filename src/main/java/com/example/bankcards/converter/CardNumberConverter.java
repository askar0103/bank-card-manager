package com.example.bankcards.converter;

import com.example.bankcards.util.CardNumberEncryptor;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;

@Converter
@RequiredArgsConstructor
public class CardNumberConverter implements AttributeConverter<String, String> {

    private final CardNumberEncryptor encryptor;

    @Override
    public String convertToDatabaseColumn(String rawCardNumber) {
        return rawCardNumber == null ? null : encryptor.encrypt(rawCardNumber);
    }

    @Override
    public String convertToEntityAttribute(String encryptedCardNumber) {
        return encryptedCardNumber == null ? null : encryptor.decrypt(encryptedCardNumber);
    }
}
