package com.example.bankcards.util;

import lombok.RequiredArgsConstructor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RequiredArgsConstructor
public class CardNumberHasher {

    private static final String HMAC_ALGO = "HmacSHA256";

    private final String secretKey;

    public String hash(String cardNumber) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGO
            );
            mac.init(secretKeySpec);

            byte[] hmacBytes = mac.doFinal(
                    cardNumber.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC", e);
        }
    }
}
