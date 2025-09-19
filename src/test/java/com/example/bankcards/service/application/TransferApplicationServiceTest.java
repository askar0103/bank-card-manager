package com.example.bankcards.service.application;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.enums.Role;
import com.example.bankcards.exception.badrequest.TransferOperationNotAllowedException;
import com.example.bankcards.exception.forbidden.CardAccessDeniedException;
import com.example.bankcards.exception.notfound.UserNotFoundException;
import com.example.bankcards.service.domain.CardDomainService;
import com.example.bankcards.service.domain.TransferDomainService;
import com.example.bankcards.service.domain.UserDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static com.example.bankcards.util.TestData.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferApplicationServiceTest {

    @Mock
    private UserDomainService userDomainService;

    @Mock
    private CardDomainService cardDomainService;

    @Mock
    private TransferDomainService transferDomainService;

    @InjectMocks
    private TransferApplicationService applicationService;

    private User user;
    private Card fromCard;
    private Card toCard;
    private TransferRequest transferRequest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(TEST_USER_ID)
                .username(TEST_USERNAME)
                .role(Role.USER)
                .build();

        fromCard = Card.builder()
                .id(TEST_CARD_ID)
                .cardNumber(TEST_CARD_NUMBER)
                .cardNumberHash(CARD_NUMBER_HASH)
                .owner(user)
                .expiryDate(FUTURE_DATE)
                .cardStatus(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .build();

        toCard = Card.builder()
                .id(TEST_CARD_ID_2)
                .cardNumber(TEST_CARD_NUMBER_2)
                .cardNumberHash(CARD_NUMBER_HASH_2)
                .owner(user)
                .expiryDate(FUTURE_DATE)
                .cardStatus(CardStatus.ACTIVE)
                .balance(new BigDecimal("500.00"))
                .build();

        transferRequest = new TransferRequest(
                fromCard.getId(),
                toCard.getId(),
                new BigDecimal("100.00"));
    }

    @Nested
    class TransferTests {

        @Test
        void transfer_WithValidRequest_ShouldCompleteTransfer() {
            when(userDomainService.getByUsername(user.getUsername())).thenReturn(user);
            when(cardDomainService.getCardByIdAndByOwnerId(fromCard.getId(), user.getId())).thenReturn(fromCard);
            when(cardDomainService.getCardByIdAndByOwnerId(toCard.getId(), user.getId())).thenReturn(toCard);
            doNothing().when(transferDomainService).transfer(fromCard, toCard, transferRequest.amount());

            applicationService.transfer(user.getUsername(), transferRequest);

            verify(userDomainService).getByUsername(user.getUsername());
            verify(cardDomainService).getCardByIdAndByOwnerId(fromCard.getId(), user.getId());
            verify(cardDomainService).getCardByIdAndByOwnerId(toCard.getId(), user.getId());
            verify(transferDomainService).transfer(fromCard, toCard, transferRequest.amount());
        }

        @Test
        void transfer_WhenFromCardNotFound_ShouldThrowCardAccessDeniedException() {
            when(userDomainService.getByUsername(user.getUsername())).thenReturn(user);
            when(cardDomainService.getCardByIdAndByOwnerId(fromCard.getId(), user.getId()))
                    .thenThrow(new CardAccessDeniedException("Card not found or access denied"));

            assertThatThrownBy(() ->
                    applicationService.transfer(user.getUsername(), transferRequest)
            ).isInstanceOf(CardAccessDeniedException.class)
                    .hasMessageContaining("Card not found or access denied");
        }

        @Test
        void transfer_WhenToCardNotFound_ShouldThrowCardAccessDeniedException() {
            when(userDomainService.getByUsername(user.getUsername())).thenReturn(user);
            when(cardDomainService.getCardByIdAndByOwnerId(fromCard.getId(), user.getId())).thenReturn(fromCard);
            when(cardDomainService.getCardByIdAndByOwnerId(toCard.getId(), user.getId()))
                    .thenThrow(new CardAccessDeniedException("Card not found or access denied"));

            assertThatThrownBy(() ->
                    applicationService.transfer(user.getUsername(), transferRequest)
            ).isInstanceOf(CardAccessDeniedException.class)
                    .hasMessageContaining("Card not found or access denied");
        }

        @Test
        void transfer_WhenUserNotFound_ShouldThrowUserNotFoundException() {
            when(userDomainService.getByUsername(user.getUsername()))
                    .thenThrow(new UserNotFoundException("User not found"));

            assertThatThrownBy(() ->
                    applicationService.transfer(user.getUsername(), transferRequest)
            ).isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        void transfer_WhenSameCard_ShouldThrowTransferOperationNotAllowedException() {
            TransferRequest sameCardRequest = new TransferRequest(toCard.getId(), toCard.getId(), new BigDecimal("100.00"));

            when(userDomainService.getByUsername(user.getUsername())).thenReturn(user);
            when(cardDomainService.getCardByIdAndByOwnerId(toCard.getId(), user.getId())).thenReturn(fromCard);
            doThrow(new TransferOperationNotAllowedException("Cannot transfer to the same card"))
                    .when(transferDomainService).transfer(any(), any(), any());

            assertThatThrownBy(() ->
                    applicationService.transfer(user.getUsername(), sameCardRequest)
            ).isInstanceOf(TransferOperationNotAllowedException.class)
                    .hasMessageContaining("Cannot transfer to the same card");
        }

        @Test
        void transfer_WhenFromCardNotActive_ShouldThrowTransferOperationNotAllowedException() {
            fromCard.setCardStatus(CardStatus.BLOCKED);

            when(userDomainService.getByUsername(user.getUsername())).thenReturn(user);
            when(cardDomainService.getCardByIdAndByOwnerId(toCard.getId(), user.getId())).thenReturn(fromCard);
            when(cardDomainService.getCardByIdAndByOwnerId(fromCard.getId(), user.getId())).thenReturn(toCard);
            doThrow(new TransferOperationNotAllowedException("Cannot transfer from card with status: BLOCKED"))
                    .when(transferDomainService).transfer(any(), any(), any());

            assertThatThrownBy(() ->
                    applicationService.transfer(user.getUsername(), transferRequest)
            ).isInstanceOf(TransferOperationNotAllowedException.class)
                    .hasMessageContaining("Cannot transfer from card with status: BLOCKED");
        }

        @Test
        void transfer_WhenToCardNotActive_ShouldThrowTransferOperationNotAllowedException() {
            toCard.setCardStatus(CardStatus.BLOCKED);

            when(userDomainService.getByUsername(user.getUsername())).thenReturn(user);
            when(cardDomainService.getCardByIdAndByOwnerId(toCard.getId(), user.getId())).thenReturn(fromCard);
            when(cardDomainService.getCardByIdAndByOwnerId(fromCard.getId(), user.getId())).thenReturn(toCard);
            doThrow(new TransferOperationNotAllowedException("Cannot transfer to card with status: BLOCKED"))
                    .when(transferDomainService).transfer(any(), any(), any());

            assertThatThrownBy(() ->
                    applicationService.transfer(user.getUsername(), transferRequest)
            ).isInstanceOf(TransferOperationNotAllowedException.class)
                    .hasMessageContaining("Cannot transfer to card with status: BLOCKED");
        }

        @Test
        void transfer_WhenAmountNotPositive_ShouldThrowTransferOperationNotAllowedException() {
            TransferRequest invalidAmountRequest = new TransferRequest(toCard.getId(), fromCard.getId(), new BigDecimal("-50.00"));

            when(userDomainService.getByUsername(user.getUsername())).thenReturn(user);
            when(cardDomainService.getCardByIdAndByOwnerId(toCard.getId(), user.getId())).thenReturn(fromCard);
            when(cardDomainService.getCardByIdAndByOwnerId(fromCard.getId(), user.getId())).thenReturn(toCard);
            doThrow(new TransferOperationNotAllowedException("Transfer amount must be positive"))
                    .when(transferDomainService).transfer(any(), any(), any());

            assertThatThrownBy(() ->
                    applicationService.transfer(user.getUsername(), invalidAmountRequest)
            ).isInstanceOf(TransferOperationNotAllowedException.class)
                    .hasMessageContaining("Transfer amount must be positive");
        }

        @Test
        void transfer_WhenInsufficientBalance_ShouldThrowTransferOperationNotAllowedException() {
            TransferRequest largeAmountRequest = new TransferRequest(toCard.getId(), fromCard.getId(), new BigDecimal("2000.00"));

            when(userDomainService.getByUsername(user.getUsername())).thenReturn(user);
            when(cardDomainService.getCardByIdAndByOwnerId(toCard.getId(), user.getId())).thenReturn(fromCard);
            when(cardDomainService.getCardByIdAndByOwnerId(fromCard.getId(), user.getId())).thenReturn(toCard);
            doThrow(new TransferOperationNotAllowedException("Insufficient balance for transfer"))
                    .when(transferDomainService).transfer(any(), any(), any());

            assertThatThrownBy(() ->
                    applicationService.transfer(user.getUsername(), largeAmountRequest)
            ).isInstanceOf(TransferOperationNotAllowedException.class)
                    .hasMessageContaining("Insufficient balance for transfer");
        }

        @Test
        void transfer_WhenCardsBelongToDifferentUsers_ShouldThrowCardAccessDeniedException() {
            User differentUser = User.builder()
                    .id(999L)
                    .username("differentUser")
                    .role(Role.USER)
                    .build();

            toCard.setOwner(differentUser);

            when(userDomainService.getByUsername(user.getUsername())).thenReturn(user);
            when(cardDomainService.getCardByIdAndByOwnerId(fromCard.getId(), user.getId()))
                    .thenThrow(new CardAccessDeniedException("Card does not belong to user"));

            assertThatThrownBy(() ->
                    applicationService.transfer(user.getUsername(), transferRequest)
            ).isInstanceOf(CardAccessDeniedException.class)
                    .hasMessageContaining("Card does not belong to user");
        }
    }
}