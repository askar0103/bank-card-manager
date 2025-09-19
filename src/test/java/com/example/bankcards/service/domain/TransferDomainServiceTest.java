package com.example.bankcards.service.domain;

import com.example.bankcards.entity.Card;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.exception.badrequest.TransferOperationNotAllowedException;
import com.example.bankcards.validator.TransferValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static com.example.bankcards.util.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferDomainServiceTest {

    @Mock
    private TransferValidator transferValidator;

    @InjectMocks
    TransferDomainService transferDomainService;

    private Card fromCard;
    private Card toCard;
    private BigDecimal transferAmount;

    @BeforeEach
    void setUp() {
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
    class TransferTests {

        @Test
        void transfer_WithValidCardsAndAmount_ShouldUpdateBalances() {
            doNothing().when(transferValidator).validateForTransfer(fromCard, toCard, transferAmount);

            BigDecimal fromCardInitialBalance = fromCard.getBalance();
            BigDecimal toCardInitialBalance = toCard.getBalance();

            transferDomainService.transfer(fromCard, toCard, transferAmount);

            assertThat(fromCard.getBalance()).isEqualTo(fromCardInitialBalance.subtract(transferAmount));
            assertThat(toCard.getBalance()).isEqualTo(toCardInitialBalance.add(transferAmount));
            verify(transferValidator).validateForTransfer(fromCard, toCard, transferAmount);
        }

        @Test
        void transfer_WithSameCard_ShouldThrowTransferOperationNotAllowedException() {
            doThrow(new TransferOperationNotAllowedException("Cannot transfer to the same card"))
                    .when(transferValidator).validateForTransfer(fromCard, fromCard, transferAmount);

            assertThatThrownBy(() ->
                    transferDomainService.transfer(fromCard, fromCard, transferAmount)
            ).isInstanceOf(TransferOperationNotAllowedException.class)
                    .hasMessageContaining("Cannot transfer to the same card");

            assertThat(fromCard.getBalance()).isEqualTo(new BigDecimal("1000.00"));
        }

        @Test
        void transfer_WithFromCardNotActive_ShouldThrowTransferOperationNotAllowedException() {
            fromCard.setCardStatus(CardStatus.BLOCKED);
            Card blockedCard = fromCard;
            doThrow(new TransferOperationNotAllowedException("Cannot transfer from card with status: BLOCKED"))
                    .when(transferValidator).validateForTransfer(blockedCard, toCard, transferAmount);

            assertThatThrownBy(() ->
                    transferDomainService.transfer(blockedCard, toCard, transferAmount)
            ).isInstanceOf(TransferOperationNotAllowedException.class)
                    .hasMessageContaining("Cannot transfer from card with status: BLOCKED");

            assertThat(blockedCard.getBalance()).isEqualTo(new BigDecimal("1000.00"));
            assertThat(toCard.getBalance()).isEqualTo(new BigDecimal("500.00"));
        }

        @Test
        void transfer_WithToCardNotActive_ShouldThrowTransferOperationNotAllowedException() {
            toCard.setCardStatus(CardStatus.BLOCKED);
            Card blockedCard = toCard;
            doThrow(new TransferOperationNotAllowedException("Cannot transfer to card with status: BLOCKED"))
                    .when(transferValidator).validateForTransfer(fromCard, blockedCard, transferAmount);

            assertThatThrownBy(() ->
                    transferDomainService.transfer(fromCard, blockedCard, transferAmount)
            ).isInstanceOf(TransferOperationNotAllowedException.class)
                    .hasMessageContaining("Cannot transfer to card with status: BLOCKED");

            assertThat(fromCard.getBalance()).isEqualTo(new BigDecimal("1000.00"));
            assertThat(blockedCard.getBalance()).isEqualTo(new BigDecimal("500.00"));
        }

        @Test
        void transfer_WithZeroAmount_ShouldThrowTransferOperationNotAllowedException() {
            BigDecimal zeroAmount = BigDecimal.ZERO;
            doThrow(new TransferOperationNotAllowedException("Transfer amount must be positive"))
                    .when(transferValidator).validateForTransfer(fromCard, toCard, zeroAmount);

            assertThatThrownBy(() ->
                    transferDomainService.transfer(fromCard, toCard, zeroAmount)
            ).isInstanceOf(TransferOperationNotAllowedException.class)
                    .hasMessageContaining("Transfer amount must be positive");

            assertThat(fromCard.getBalance()).isEqualTo(new BigDecimal("1000.00"));
            assertThat(toCard.getBalance()).isEqualTo(new BigDecimal("500.00"));
        }

        @Test
        void transfer_WithNegativeAmount_ShouldThrowTransferOperationNotAllowedException() {
            BigDecimal negativeAmount = new BigDecimal("-50.00");
            doThrow(new TransferOperationNotAllowedException("Transfer amount must be positive"))
                    .when(transferValidator).validateForTransfer(fromCard, toCard, negativeAmount);

            assertThatThrownBy(() ->
                    transferDomainService.transfer(fromCard, toCard, negativeAmount)
            ).isInstanceOf(TransferOperationNotAllowedException.class)
                    .hasMessageContaining("Transfer amount must be positive");

            assertThat(fromCard.getBalance()).isEqualTo(new BigDecimal("1000.00"));
            assertThat(toCard.getBalance()).isEqualTo(new BigDecimal("500.00"));
        }

        @Test
        void transfer_WithInsufficientBalance_ShouldThrowTransferOperationNotAllowedException() {
            BigDecimal largeAmount = new BigDecimal("2000.00");
            doThrow(new TransferOperationNotAllowedException("Insufficient balance for transfer"))
                    .when(transferValidator).validateForTransfer(fromCard, toCard, largeAmount);

            assertThatThrownBy(() ->
                    transferDomainService.transfer(fromCard, toCard, largeAmount)
            ).isInstanceOf(TransferOperationNotAllowedException.class)
                    .hasMessageContaining("Insufficient balance for transfer");

            assertThat(fromCard.getBalance()).isEqualTo(new BigDecimal("1000.00"));
            assertThat(toCard.getBalance()).isEqualTo(new BigDecimal("500.00"));
        }

        @Test
        void transfer_WithExactBalance_ShouldUpdateBalances() {
            BigDecimal exactAmount = new BigDecimal("1000.00");
            doNothing().when(transferValidator).validateForTransfer(fromCard, toCard, exactAmount);

            BigDecimal fromCardInitialBalance = fromCard.getBalance();
            BigDecimal toCardInitialBalance = toCard.getBalance();

            transferDomainService.transfer(fromCard, toCard, exactAmount);

            assertThat(fromCard.getBalance()).isEqualTo(fromCardInitialBalance.subtract(exactAmount));
            assertThat(toCard.getBalance()).isEqualTo(toCardInitialBalance.add(exactAmount));
            verify(transferValidator).validateForTransfer(fromCard, toCard, exactAmount);
        }

        @Test
        void transfer_WithDecimalAmount_ShouldUpdateBalancesCorrectly() {
            BigDecimal decimalAmount = new BigDecimal("123.45");
            doNothing().when(transferValidator).validateForTransfer(fromCard, toCard, decimalAmount);

            BigDecimal fromCardInitialBalance = fromCard.getBalance();
            BigDecimal toCardInitialBalance = toCard.getBalance();

            transferDomainService.transfer(fromCard, toCard, decimalAmount);

            assertThat(fromCard.getBalance()).isEqualTo(fromCardInitialBalance.subtract(decimalAmount));
            assertThat(toCard.getBalance()).isEqualTo(toCardInitialBalance.add(decimalAmount));
            verify(transferValidator).validateForTransfer(fromCard, toCard, decimalAmount);
        }
    }
}