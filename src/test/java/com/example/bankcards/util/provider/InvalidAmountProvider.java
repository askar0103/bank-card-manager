package com.example.bankcards.util.provider;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.math.BigDecimal;
import java.util.stream.Stream;

public class InvalidAmountProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        return Stream.of(
                Arguments.of((BigDecimal) null),
                Arguments.of(BigDecimal.ZERO),
                Arguments.of(new BigDecimal("-100.00")),
                Arguments.of(new BigDecimal("0.00")),
                Arguments.of(new BigDecimal("0.001"))
        );
    }
}
