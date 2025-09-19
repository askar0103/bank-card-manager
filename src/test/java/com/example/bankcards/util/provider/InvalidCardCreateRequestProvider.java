package com.example.bankcards.util.provider;

import com.example.bankcards.dto.request.CardCreateRequest;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.stream.Stream;

import static com.example.bankcards.util.TestData.*;
import static com.example.bankcards.util.TestData.NEGATIVE_BALANCE;

public class InvalidCardCreateRequestProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        return Stream.of(

                // CardNumber is null
                Arguments.of(new CardCreateRequest(
                        null, TEST_USER_ID, FUTURE_DATE, POSITIVE_BALANCE)),

                // CardNumber is empty
                Arguments.of(new CardCreateRequest(
                        "", TEST_USER_ID, FUTURE_DATE, POSITIVE_BALANCE)),

                // CardNumber is invalid
                Arguments.of(new CardCreateRequest(
                        "abc", TEST_USER_ID, FUTURE_DATE, POSITIVE_BALANCE)),

                // OwnerId is negative
                Arguments.of(new CardCreateRequest(
                        TEST_CARD_NUMBER, -1L, FUTURE_DATE, POSITIVE_BALANCE)),

                // ExpiryDate is in the past
                Arguments.of(new CardCreateRequest(
                        TEST_CARD_NUMBER, TEST_USER_ID, PAST_DATE, POSITIVE_BALANCE)),

                // InitialBalance is negative
                Arguments.of(new CardCreateRequest(
                        TEST_CARD_NUMBER, TEST_USER_ID, FUTURE_DATE, NEGATIVE_BALANCE))
        );
    }
}
