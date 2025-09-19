package com.example.bankcards.util.provider;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.stream.Stream;

import static com.example.bankcards.util.TestData.TEST_USERNAME;
import static com.example.bankcards.util.TestData.TEST_USER_PASSWORD;

public class InvalidUsernamePasswordProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        return Stream.of(
                // Username is null
                Arguments.of(null, TEST_USER_PASSWORD),

                // Username is empty
                Arguments.of("", TEST_USER_PASSWORD),

                // Username is too short
                Arguments.of("u", TEST_USER_PASSWORD),

                // Username is too long
                Arguments.of("u".repeat(40), TEST_USER_PASSWORD),

                // Password is null
                Arguments.of(TEST_USERNAME, null),

                // Password is empty
                Arguments.of(TEST_USERNAME, ""),

                // Password is too short
                Arguments.of(TEST_USERNAME, "p"),

                // Password is too long
                Arguments.of(TEST_USERNAME, "p".repeat(40))
        );
    }
}
