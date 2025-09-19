package com.example.bankcards.validator;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.enums.Role;
import com.example.bankcards.exception.badrequest.CardDataNotValidException;
import com.example.bankcards.exception.badrequest.CardOperationNotAllowedException;
import com.example.bankcards.exception.conflict.CardAlreadyExistsException;
import com.example.bankcards.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static com.example.bankcards.util.TestData.*;
import static com.example.bankcards.util.TestData.POSITIVE_BALANCE;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardValidatorTest {

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CardValidator cardValidator;

    private Card card;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id(TEST_USER_ID)
                .username(TEST_USERNAME)
                .passwordHash(TEST_PASSWORD_HASH)
                .role(Role.USER)
                .build();

        card = Card.builder()
                .id(TEST_CARD_ID)
                .cardNumber(TEST_CARD_NUMBER)
                .cardNumberHash(CARD_NUMBER_HASH)
                .owner(user)
                .expiryDate(FUTURE_DATE)
                .cardStatus(CardStatus.ACTIVE)
                .balance(POSITIVE_BALANCE)
                .build();

        user.getCards().add(card);
    }

    @Nested
    class ValidateForCreateTests {

        @Test
        void validateForCreate_WithValidCard_ShouldNotThrowException() {
            when(cardRepository.existsByCardNumberHash(CARD_NUMBER_HASH)).thenReturn(false);

            cardValidator.validateForCreate(card);
            verify(cardRepository).existsByCardNumberHash(CARD_NUMBER_HASH);
        }

        @Test
        void validateForCreate_WithAdminOwner_ShouldThrowCardOperationNotAllowedException() {
            card.getOwner().setRole(Role.ADMIN);
            Card adminCard = card;

            assertThatThrownBy(() ->
                    cardValidator.validateForCreate(adminCard)
            ).isInstanceOf(CardOperationNotAllowedException.class)
                    .hasMessage("Cannot create a card for ADMIN user");

            verify(cardRepository, never()).existsByCardNumberHash(anyString());
        }

        @Test
        void validateForCreate_WithExpiredDate_ShouldThrowCardDataNotValidException() {
            card.setExpiryDate(PAST_DATE);
            card.setCardStatus(CardStatus.EXPIRED);
            Card expiredCard = card;

            assertThatThrownBy(() ->
                    cardValidator.validateForCreate(expiredCard)
            ).isInstanceOf(CardDataNotValidException.class)
                    .hasMessage("Expiration date must be in the future");

            verify(cardRepository, never()).existsByCardNumberHash(CARD_NUMBER_HASH);
        }

        @Test
        void validateForCreate_WithExistingCardNumber_ShouldThrowCardAlreadyExistsException() {
            when(cardRepository.existsByCardNumberHash(CARD_NUMBER_HASH)).thenReturn(true);

            assertThatThrownBy(() ->
                    cardValidator.validateForCreate(card)
            ).isInstanceOf(CardAlreadyExistsException.class)
                    .hasMessageContaining("Card with number")
                    .hasMessageContaining("already exists");

            verify(cardRepository).existsByCardNumberHash(CARD_NUMBER_HASH);
        }
    }

    @Nested
    class ValidateForBlockTests {

        @Test
        void validateForBlock_WithActiveCard_ShouldNotThrowException() {
            cardValidator.validateForBlock(card);
        }

        @Test
        void validateForBlock_WithBlockedCard_ShouldThrowCardOperationNotAllowedException() {
            card.setCardStatus(CardStatus.BLOCKED);
            Card blockedCard = card;

            assertThatThrownBy(() ->
                    cardValidator.validateForBlock(blockedCard)
            ).isInstanceOf(CardOperationNotAllowedException.class)
                    .hasMessage("Cannot block card with status: BLOCKED");
        }

        @Test
        void validateForBlock_WithExpiredCard_ShouldThrowCardOperationNotAllowedException() {
            card.setExpiryDate(PAST_DATE);
            card.setCardStatus(CardStatus.EXPIRED);
            Card expiredCard = card;

            assertThatThrownBy(() ->
                    cardValidator.validateForBlock(expiredCard)
            ).isInstanceOf(CardOperationNotAllowedException.class)
                    .hasMessage("Cannot block card with status: EXPIRED");
        }
    }

    @Nested
    class ValidateForActivateTests {

        @Test
        void validateForActivate_WithBlockedCard_ShouldNotThrowException() {
            card.setCardStatus(CardStatus.BLOCKED);
            cardValidator.validateForActivate(card);
        }

        @Test
        void validateForActivate_WithActiveCard_ShouldThrowCardOperationNotAllowedException() {
            assertThatThrownBy(() ->
                    cardValidator.validateForActivate(card)
            ).isInstanceOf(CardOperationNotAllowedException.class)
                    .hasMessage("Cannot activate card with status: ACTIVE");
        }

        @Test
        void validateForActivate_WithExpiredCard_ShouldThrowCardOperationNotAllowedException() {
            card.setExpiryDate(PAST_DATE);
            card.setCardStatus(CardStatus.EXPIRED);
            assertThatThrownBy(() ->
                    cardValidator.validateForActivate(card)
            ).isInstanceOf(CardOperationNotAllowedException.class)
                    .hasMessage("Cannot activate card with status: EXPIRED");
        }
    }

    @Nested
    class ValidateForDeleteTests {

        @Test
        void validateForDelete_WithBlockedCardAndZeroBalance_ShouldNotThrowException() {
            card.setBalance(BigDecimal.ZERO);
            card.setCardStatus(CardStatus.BLOCKED);
            cardValidator.validateForDelete(card);
        }

        @Test
        void validateForDelete_WithActiveCard_ShouldThrowCardOperationNotAllowedException() {
            assertThatThrownBy(() ->
                    cardValidator.validateForDelete(card)
            ).isInstanceOf(CardOperationNotAllowedException.class)
                    .hasMessage("Cannot delete card with status: ACTIVE. Card must be blocked first");
        }

        @Test
        void validateForDelete_WithBlockedCardAndNonZeroBalance_ShouldThrowCardOperationNotAllowedException() {
            card.setCardStatus(CardStatus.BLOCKED);
            assertThatThrownBy(() ->
                    cardValidator.validateForDelete(card)
            ).isInstanceOf(CardOperationNotAllowedException.class)
                    .hasMessage("Cannot delete card with non-zero balance: 100.00");
        }

        @Test
        void validateForDelete_WithExpiredCardAndZeroBalance_ShouldNotThrowException() {
            card.setBalance(BigDecimal.ZERO);
            card.setExpiryDate(PAST_DATE);
            card.setCardStatus(CardStatus.EXPIRED);
            cardValidator.validateForDelete(card);
        }

        @Test
        void validateForDelete_WithExpiredCardAndNonZeroBalance_ShouldThrowCardOperationNotAllowedException() {
            card.setExpiryDate(PAST_DATE);
            card.setCardStatus(CardStatus.EXPIRED);

            assertThatThrownBy(() ->
                    cardValidator.validateForDelete(card)
            ).isInstanceOf(CardOperationNotAllowedException.class)
                    .hasMessage("Cannot delete card with non-zero balance: 100.00");
        }
    }
}