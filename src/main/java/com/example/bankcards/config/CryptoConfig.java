package com.example.bankcards.config;

import com.example.bankcards.util.CardNumberEncryptor;
import com.example.bankcards.util.CardNumberHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CryptoConfig {

    @Bean
    public CardNumberEncryptor cardNumberEncryptor(
            @Value("${crypto.encryptor.password}") String password,
            @Value("${crypto.encryptor.salt}") String salt
    ) {
        return new CardNumberEncryptor(password, salt);
    }

    @Bean
    public CardNumberHasher cardNumberHasher(
            @Value("${crypto.hasher.secret-key}") String secretKey
    ) {
        return new CardNumberHasher(secretKey);
    }
}
