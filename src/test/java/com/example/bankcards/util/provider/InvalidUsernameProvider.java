package com.example.bankcards.util.provider;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.stream.Stream;

public class InvalidUsernameProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        return Stream.of(
                // Username is null
                Arguments.of((String) null),

                // Username is empty
                Arguments.of(""),

                // Username is too short
                Arguments.of("u"),

                // Username is too long
                Arguments.of("u".repeat(40))
        );
    }
}
