package com.example.bankcards.util.provider;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.stream.Stream;

public class InvalidPasswordProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        return Stream.of(
                // Password is null
                Arguments.of((String) null),

                // Password is empty
                Arguments.of( ""),

                // Password is too short
                Arguments.of( "p"),

                // Password is too long
                Arguments.of( "p".repeat(40))
        );
    }
}
