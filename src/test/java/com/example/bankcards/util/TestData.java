package com.example.bankcards.util;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class TestData {

    private TestData() {
    }

    public static final Long TEST_USER_ID = 1L;
    public static final String TEST_USERNAME = "TestUser";
    public static final String TEST_USER_PASSWORD = "password";
    public static final String TEST_PASSWORD_HASH = "passwordHash";
    public static final String NEW_USERNAME = "NewTestUser";
    public static final String NEW_USER_PASSWORD = "newPassword";

    public static final String TEST_TOKEN = "valid.jwt.token";

    public static final Long TEST_CARD_ID = 1L;
    public static final Long TEST_CARD_ID_2 = 2L;
    public static final String TEST_CARD_NUMBER = "1111 1111 1111 1111";
    public static final String TEST_CARD_NUMBER_2 = "2222 2222 2222 2222";
    public static final String MASKED_CARD_NUMBER = CardNumberMasker.mask(TEST_CARD_NUMBER);
    public static final String CARD_NUMBER_HASH = "hashed-card-number";
    public static final String CARD_NUMBER_HASH_2 = "hashed-card-number-2";

    public static final BigDecimal POSITIVE_BALANCE = new BigDecimal("100.00");
    public static final BigDecimal NEGATIVE_BALANCE = new BigDecimal("-100.00");

    public static final LocalDate FUTURE_DATE = LocalDate.now().plusYears(2);
    public static final LocalDate PAST_DATE = LocalDate.now().minusYears(2);
}
