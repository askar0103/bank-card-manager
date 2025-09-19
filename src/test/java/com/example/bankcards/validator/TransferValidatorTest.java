package com.example.bankcards.validator;

import com.example.bankcards.entity.Card;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.exception.badrequest.TransferOperationNotAllowedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static com.example.bankcards.util.TestData.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class TransferValidatorTest {

    private TransferValidator transferValidator;

    private Card fromCard;
    private Card toCard;
    private BigDecimal transferAmount;

    @BeforeEach
    void setUp() {
        transferValidator = new TransferValidator();

        fromCard = Card.builder()
                .id(TEST_CARD_ID)
                .cardNumber(TEST_CARD_NUMBER)
                .cardNumberHash(CARD_NUMBER_HASH)
                .expiryDate(FUTURE_DATE)
                .cardStatus(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .build();

        toCard = Card.builder()
                .id(TEST_CARD_ID_2)
                .cardNumber(TEST_CARD_NUMBER_2)
                .cardNumberHash(CARD_NUMBER_HASH_2)
                .expiryDate(FUTURE_DATE)
                .cardStatus(CardStatus.ACTIVE)
                .balance(new BigDecimal("500.00"))
                .build();

        transferAmount = new BigDecimal("100.00");
    }

    @Nested
    class ValidateForTransferTest {

        @Test
        void validateForTransfer_WithValidCardsAndAmount_ShouldNotThrowException() {
            transferValidator.validateForTransfer(fromCard, toCard, transferAmount);
        }

        @Test
        void validateForTransfer_WithSameCard_ShouldThrowTransferOperationNotAllowedException() {
            assertThatThrownBy(() ->
                    transferValidator.validateForTransfer(fromCard, fromCard, transferAmount)
            ).isInstanceOf(TransferOperationNotAllowedException.class)
                    .hasMessage("Cannot transfer to the same card");
        }

        @Test
        void validateForTransfer_WithFromCardBlocked_ShouldThrowTransferOperationNotAllowedException() {
            fromCard.setCardStatus(CardStatus.BLOCKED);
            Card blockedCard = fromCard;

            assertThatThrownBy(() ->
                    transferValidator.validateForTransfer(blockedCard, toCard, transferAmount)
            ).isInstanceOf(TransferOperationNotAllowedException.class)
                    .hasMessage("Cannot transfer from card with status: BLOCKED");
        }

        @Test
        void validateForTransfer_WithFromCardExpired_ShouldThrowTransferOperationNotAllowedException() {
            fromCard.setExpiryDate(PAST_DATE);
            fromCard.setCardStatus(CardStatus.EXPIRED);
            Card expiredCard = fromCard;

            assertThatThrownBy(() ->
                    transferValidator.validateForTransfer(expiredCard, toCard, transferAmount)
            ).isInstanceOf(TransferOperationNotAllowedException.class)
                    .hasMessage("Cannot transfer from card with status: EXPIRED");
        }

        @Test
        void validateForTransfer_WithToCardBlocked_ShouldThrowTransferOperationNotAllowedException() {
            toCard.setCardStatus(CardStatus.BLOCKED);
            Card blockedCard = toCard;

            assertThatThrownBy(() ->
                    transferValidator.validateForTransfer(fromCard, blockedCard, transferAmount)
            ).isInstanceOf(TransferOperationNotAllowedException.class)
                    .hasMessage("Cannot transfer to card with status: BLOCKED");
        }

        @Test
        void validateForTransfer_WithToCardExpired_ShouldThrowTransferOperationNotAllowedException() {
            toCard.setExpiryDate(PAST_DATE);
            toCard.setCardStatus(CardStatus.EXPIRED);
            Card expiredCard = toCard;

            assertThatThrownBy(() ->
                    transferValidator.validateForTransfer(fromCard, expiredCard, transferAmount)
            ).isInstanceOf(TransferOperationNotAllowedException.class)
                    .hasMessage("Cannot transfer to card with status: EXPIRED");
        }

        @Test
        void validateForTransfer_WithZeroAmount_ShouldThrowTransferOperationNotAllowedException() {
            BigDecimal zeroAmount = BigDecimal.ZERO;

            assertThatThrownBy(() ->
                    transferValidator.validateForTransfer(fromCard, toCard, zeroAmount)
            ).isInstanceOf(TransferOperationNotAllowedException.class)
                    .hasMessage("Transfer amount must be positive");
        }

        @Test
        void validateForTransfer_WithNegativeAmount_ShouldThrowTransferOperationNotAllowedException() {
            BigDecimal negativeAmount = new BigDecimal("-50.00");

            assertThatThrownBy(() ->
                    transferValidator.validateForTransfer(fromCard, toCard, negativeAmount)
            ).isInstanceOf(TransferOperationNotAllowedException.class)
                    .hasMessage("Transfer amount must be positive");
        }

        @Test
        void validateForTransfer_WithInsufficientBalance_ShouldThrowTransferOperationNotAllowedException() {
            BigDecimal largeAmount = new BigDecimal("2000.00");

            assertThatThrownBy(() ->
                    transferValidator.validateForTransfer(fromCard, toCard, largeAmount)
            ).isInstanceOf(TransferOperationNotAllowedException.class)
                    .hasMessage("Insufficient balance for transfer. Available: 1000.00, Required: 2000.00");
        }

        @Test
        void validateForTransfer_WithExactBalance_ShouldNotThrowException() {
            BigDecimal exactAmount = new BigDecimal("1000.00");

            transferValidator.validateForTransfer(fromCard, toCard, exactAmount);
        }

        @Test
        void validateForTransfer_WithDecimalAmount_ShouldNotThrowException() {
            BigDecimal decimalAmount = new BigDecimal("123.45");

            transferValidator.validateForTransfer(fromCard, toCard, decimalAmount);
        }

        @Test
        void validateForTransfer_WithDifferentCardStatusCombinations_ShouldThrowAppropriateExceptions() {
            fromCard.setCardStatus(CardStatus.BLOCKED);
            toCard.setExpiryDate(PAST_DATE);
            toCard.setCardStatus(CardStatus.EXPIRED);

            Card blockedCard = fromCard;
            Card expiredCard = toCard;

            assertThatThrownBy(() ->
                    transferValidator.validateForTransfer(blockedCard, expiredCard, transferAmount)
            ).isInstanceOf(TransferOperationNotAllowedException.class)
                    .hasMessage("Cannot transfer from card with status: BLOCKED");
        }

        @Test
        void validateForTransfer_WithVeryLargeAmount_ShouldThrowTransferOperationNotAllowedException() {
            BigDecimal veryLargeAmount = new BigDecimal("999999999.99");

            assertThatThrownBy(() ->
                    transferValidator.validateForTransfer(fromCard, toCard, veryLargeAmount)
            ).isInstanceOf(TransferOperationNotAllowedException.class)
                    .hasMessage("Insufficient balance for transfer. Available: 1000.00, Required: 999999999.99");
        }
    }
}