package com.example.bankcards.service.domain;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.enums.Role;
import com.example.bankcards.exception.badrequest.CardDataNotValidException;
import com.example.bankcards.exception.badrequest.CardOperationNotAllowedException;
import com.example.bankcards.exception.conflict.CardAlreadyExistsException;
import com.example.bankcards.exception.notfound.CardNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.validator.CardValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.example.bankcards.util.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardDomainServiceTest {

    @Mock
    private CardValidator cardValidator;

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CardDomainService cardDomainService;

    private User user;
    private Card card;

    @BeforeEach
    void setUp() {
        user = User.builder()
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
    class CreateCardTests {

        @Test
        void createCard_WithValidCard_ShouldReturnSavedCard() {
            doNothing().when(cardValidator).validateForCreate(card);
            when(cardRepository.save(card)).thenReturn(card);

            Card result = cardDomainService.createCard(card);

            assertThat(result).isEqualTo(card);
            verify(cardValidator).validateForCreate(card);
            verify(cardRepository).save(card);
        }

        @Test
        void createCard_WithAdminOwner_ShouldThrowCardOperationNotAllowedException() {
            user.setRole(Role.ADMIN);
            String expectedMessage = "Cannot create a card for ADMIN user";

            doThrow(new CardOperationNotAllowedException(expectedMessage))
                    .when(cardValidator).validateForCreate(card);

            assertThatThrownBy(() ->
                    cardDomainService.createCard(card)
            ).isInstanceOf(CardOperationNotAllowedException.class)
                    .hasMessageContaining(expectedMessage);
        }

        @Test
        void createCard_WithExpiredDate_ShouldThrowCardDataNotValidException() {
            card.setExpiryDate(PAST_DATE);
            card.setCardStatus(CardStatus.EXPIRED);
            String expectedMessage = "Expiration date must be in the future";

            doThrow(new CardDataNotValidException(expectedMessage))
                    .when(cardValidator).validateForCreate(card);

            assertThatThrownBy(() ->
                    cardDomainService.createCard(card)
            ).isInstanceOf(CardDataNotValidException.class)
                    .hasMessageContaining(expectedMessage);
        }

        @Test
        void createCard_WithExistingCardNumber_ShouldThrowCardAlreadyExistsException() {
            String expectedMessage = "Card with number %s already exists".formatted(MASKED_CARD_NUMBER);

            doThrow(new CardAlreadyExistsException(expectedMessage))
                    .when(cardValidator).validateForCreate(card);

            assertThatThrownBy(() ->
                    cardDomainService.createCard(card)
            ).isInstanceOf(CardAlreadyExistsException.class)
                    .hasMessageContaining(expectedMessage);
        }
    }

    @Nested
    class GetCardByIdTests {

        @Test
        void getCardById_WithExistingId_ShouldReturnCard() {
            when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));

            Card result = cardDomainService.getCardById(card.getId());

            assertThat(result).isEqualTo(card);
            verify(cardRepository).findById(card.getId());
        }

        @Test
        void getCardById_WithNonExistingId_ShouldThrowCardNotFoundException() {
            when(cardRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    cardDomainService.getCardById(999L)
            ).isInstanceOf(CardNotFoundException.class)
                    .hasMessageContaining("Card with id 999 not found");
        }

        @Test
        void getCardById_WithExpiredCard_ShouldUpdateStatusToExpired() {
            card.setExpiryDate(PAST_DATE);
            when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));
            when(cardRepository.save(any())).thenAnswer(invocation -> {
                Card savedCard = invocation.getArgument(0);
                assertThat(savedCard.getCardStatus()).isEqualTo(CardStatus.EXPIRED);
                return savedCard;
            });

            Card result = cardDomainService.getCardById(card.getId());

            assertThat(result.getCardStatus()).isEqualTo(CardStatus.EXPIRED);
            verify(cardRepository).save(card);
        }

    }

    @Nested
    class GetCardByIdAndByOwnerIdTests {

        @Test
        void getCardByIdAndByOwnerId_WithValidIds_ShouldReturnCard() {
            when(cardRepository.findByIdAndOwner_Id(card.getId(), user.getId())).thenReturn(Optional.of(card));

            Card result = cardDomainService.getCardByIdAndByOwnerId(card.getId(), user.getId());

            assertThat(result).isEqualTo(card);
            verify(cardRepository).findByIdAndOwner_Id(card.getId(), user.getId());
        }

        @Test
        void getCardByIdAndByOwnerId_WithNonExistingCard_ShouldThrowCardNotFoundException() {
            when(cardRepository.findByIdAndOwner_Id(999L, user.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    cardDomainService.getCardByIdAndByOwnerId(999L, user.getId())
            ).isInstanceOf(CardNotFoundException.class)
                    .hasMessageContaining("Card not found or does not belong to the user");
        }

        @Test
        void getCardByIdAndByOwnerId_WithExpiredCard_ShouldUpdateStatusToExpired() {
            card.setExpiryDate(PAST_DATE);

            when(cardRepository.findByIdAndOwner_Id(card.getId(), user.getId())).thenReturn(Optional.of(card));
            when(cardRepository.save(any())).thenAnswer(invocation -> {
                Card savedCard = invocation.getArgument(0);
                assertThat(savedCard.getCardStatus()).isEqualTo(CardStatus.EXPIRED);
                return savedCard;
            });

            Card result = cardDomainService.getCardByIdAndByOwnerId(card.getId(), user.getId());

            assertThat(result.getCardStatus()).isEqualTo(CardStatus.EXPIRED);
            verify(cardRepository).save(card);
        }
    }

    @Nested
    class GetCardsTests {

        @Test
        void getCards_WithPageable_ShouldReturnPageOfCards() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Card> cardPage = new PageImpl<>(List.of(card));

            when(cardRepository.findAll(pageable)).thenReturn(cardPage);

            Page<Card> result = cardDomainService.getCards(pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(cardRepository).findAll(pageable);
        }
    }

    @Nested
    class GetCardsByOwnerTests {

        @Test
        void getCardsByOwner_WithValidOwner_ShouldReturnPageOfCards() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Card> cardPage = new PageImpl<>(List.of(card));

            when(cardRepository.findAllByOwner(user, pageable)).thenReturn(cardPage);

            Page<Card> result = cardDomainService.getCardsByOwner(user, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(cardRepository).findAllByOwner(user, pageable);
        }
    }

    @Nested
    class BlockCardTests {

        @Test
        void blockCard_WithActiveCard_ShouldBlockCard() {
            doNothing().when(cardValidator).validateForBlock(card);

            Card result = cardDomainService.blockCard(card);

            assertThat(result.getCardStatus()).isEqualTo(CardStatus.BLOCKED);
            verify(cardValidator).validateForBlock(card);
        }

        @Test
        void blockCard_WithNonActiveCard_ShouldThrowCardOperationNotAllowedException() {
            card.setCardStatus(CardStatus.BLOCKED);
            String expectedMessage = "Cannot block card with status: BLOCKED";

            doThrow(new CardOperationNotAllowedException(expectedMessage))
                    .when(cardValidator).validateForBlock(card);

            assertThatThrownBy(() ->
                    cardDomainService.blockCard(card)
            ).isInstanceOf(CardOperationNotAllowedException.class)
                    .hasMessageContaining(expectedMessage);
        }
    }

    @Nested
    class ActivateCardTests {

        @Test
        void activateCard_WithBlockedCard_ShouldActivateCard() {
            card.setCardStatus(CardStatus.BLOCKED);
            doNothing().when(cardValidator).validateForActivate(card);

            Card result = cardDomainService.activateCard(card);

            assertThat(result.getCardStatus()).isEqualTo(CardStatus.ACTIVE);
            verify(cardValidator).validateForActivate(card);
        }

        @Test
        void activateCard_WithNonBlockedCard_ShouldThrowCardOperationNotAllowedException() {
            String expectedMessage = "Cannot activate card with status: ACTIVE";
            doThrow(new CardOperationNotAllowedException(expectedMessage))
                    .when(cardValidator).validateForActivate(card);

            assertThatThrownBy(() ->
                    cardDomainService.activateCard(card)
            ).isInstanceOf(CardOperationNotAllowedException.class)
                    .hasMessageContaining(expectedMessage);
        }
    }

    @Nested
    class DeleteCardTests {

        @Test
        void deleteCard_WithBlockedCardAndZeroBalance_ShouldDeleteCard() {
            card.setBalance(BigDecimal.ZERO);
            card.setCardStatus(CardStatus.BLOCKED);

            doNothing().when(cardValidator).validateForDelete(card);
            doNothing().when(cardRepository).delete(card);

            cardDomainService.deleteCard(card);

            verify(cardValidator).validateForDelete(card);
            verify(cardRepository).delete(card);
        }

        @Test
        void deleteCard_WithActiveCard_ShouldThrowCardOperationNotAllowedException() {
            String expectedMessage = "Cannot delete card with status: ACTIVE";
            doThrow(new CardOperationNotAllowedException(expectedMessage))
                    .when(cardValidator).validateForDelete(card);

            assertThatThrownBy(() ->
                    cardDomainService.deleteCard(card)
            ).isInstanceOf(CardOperationNotAllowedException.class)
                    .hasMessageContaining(expectedMessage);
        }

        @Test
        void deleteCard_WithNonZeroBalance_ShouldThrowCardOperationNotAllowedException() {
            card.setCardStatus(CardStatus.BLOCKED);

            String expectedMessage = "Cannot delete card with non-zero balance: %s".formatted(card.getBalance());
            doThrow(new CardOperationNotAllowedException(expectedMessage))
                    .when(cardValidator).validateForDelete(card);

            assertThatThrownBy(() ->
                    cardDomainService.deleteCard(card)
            ).isInstanceOf(CardOperationNotAllowedException.class)
                    .hasMessageContaining(expectedMessage);
        }
    }

    @Nested
    class UpdateStatusIfExpiredTests {

        @Test
        void updateStatusIfExpired_WithExpiredCard_ShouldUpdateStatus() {
            card.setExpiryDate(PAST_DATE);

            when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));
            when(cardRepository.save(any(Card.class))).thenReturn(card);

            Card result = cardDomainService.getCardById(card.getId());

            assertThat(result.getCardStatus()).isEqualTo(CardStatus.EXPIRED);
        }

        @Test
        void updateStatusIfExpired_WithNonExpiredCard_ShouldNotUpdateStatus() {
            when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));

            Card result = cardDomainService.getCardById(card.getId());

            assertThat(result.getCardStatus()).isEqualTo(CardStatus.ACTIVE);
            verify(cardRepository, never()).save(card);
        }

        @Test
        void updateStatusIfExpired_WithAlreadyExpiredCard_ShouldNotUpdateStatus() {
            card.setExpiryDate(PAST_DATE);
            card.setCardStatus(CardStatus.EXPIRED);

            when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));

            Card result = cardDomainService.getCardById(card.getId());

            assertThat(result.getCardStatus()).isEqualTo(CardStatus.EXPIRED);
            verify(cardRepository, never()).save(card);
        }
    }
}