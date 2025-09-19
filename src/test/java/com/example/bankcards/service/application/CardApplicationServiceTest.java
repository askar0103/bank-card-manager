package com.example.bankcards.service.application;

import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.dto.response.UserResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.enums.Role;
import com.example.bankcards.exception.badrequest.CardOperationNotAllowedException;
import com.example.bankcards.exception.conflict.CardAlreadyExistsException;
import com.example.bankcards.exception.forbidden.CardAccessDeniedException;
import com.example.bankcards.exception.notfound.CardNotFoundException;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.mapper.UserMapper;
import com.example.bankcards.service.domain.CardDomainService;
import com.example.bankcards.service.domain.UserDomainService;
import com.example.bankcards.util.CardNumberHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.example.bankcards.util.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardApplicationServiceTest {

    @Mock
    private UserDomainService userDomainService;

    @Spy
    private UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @Mock
    private CardDomainService cardDomainService;

    @Spy
    private CardMapper cardMapper = Mappers.getMapper(CardMapper.class);

    @Mock
    private CardNumberHasher cardNumberHasher;

    @InjectMocks
    private CardApplicationService applicationService;

    private User user;
    private Card card;
    private CardCreateRequest cardCreateRequest;
    private CardResponse cardResponse;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(TEST_USER_ID)
                .username(TEST_USERNAME)
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

        cardCreateRequest = new CardCreateRequest(
                TEST_CARD_NUMBER,
                TEST_USER_ID,
                FUTURE_DATE,
                POSITIVE_BALANCE
        );

        userResponse = new UserResponse(
                TEST_USER_ID,
                TEST_USERNAME,
                Role.USER
        );

        cardResponse = new CardResponse(
                TEST_CARD_ID,
                MASKED_CARD_NUMBER,
                FUTURE_DATE,
                CardStatus.ACTIVE,
                POSITIVE_BALANCE
        );
    }

    @Nested
    class CreateCardTests {

        @Test
        void createCard_WithValidRequest_ShouldReturnCardResponse() {
            when(userDomainService.getUserById(user.getId())).thenReturn(user);
            when(cardNumberHasher.hash(card.getCardNumber())).thenReturn(CARD_NUMBER_HASH);
            when(cardDomainService.createCard(any(Card.class))).thenReturn(card);

            CardResponse result = applicationService.createCard(cardCreateRequest);

            assertThat(result).isEqualTo(cardResponse);
            verify(userDomainService).getUserById(user.getId());
            verify(cardNumberHasher).hash(card.getCardNumber());
            verify(cardDomainService).createCard(any(Card.class));
        }

        @Test
        void createCard_ForAdminUser_ShouldThrowCardOperationNotAllowedException() {
            String expectedMessage = "Cannot create a card for ADMIN user";

            user.setRole(Role.ADMIN);
            when(userDomainService.getUserById(user.getId())).thenReturn(user);
            when(cardNumberHasher.hash(card.getCardNumber())).thenReturn(card.getCardNumberHash());
            when(cardDomainService.createCard(any(Card.class))).thenThrow(
                    new CardOperationNotAllowedException(expectedMessage)
            );

            assertThatThrownBy(() ->
                    applicationService.createCard(cardCreateRequest)
            ).isInstanceOf(CardOperationNotAllowedException.class)
                    .hasMessageContaining(expectedMessage);
        }

        @Test
        void createCard_WhenCardNumberAlreadyExists_ShouldThrowCardAlreadyExistsException() {
            String expectedMessage = "Card with number %s already exists".formatted(MASKED_CARD_NUMBER);

            when(userDomainService.getUserById(user.getId())).thenReturn(user);
            when(cardNumberHasher.hash(card.getCardNumber())).thenReturn(card.getCardNumberHash());
            when(cardDomainService.createCard(any(Card.class))).thenThrow(
                    new CardAlreadyExistsException(expectedMessage)
            );

            assertThatThrownBy(() ->
                    applicationService.createCard(cardCreateRequest)
            ).isInstanceOf(CardAlreadyExistsException.class)
                    .hasMessageContaining(expectedMessage);
        }
    }

    @Nested
    class GetCardByIdForUserTests {

        @Test
        void getCardByIdForUser_WhenUserIsOwner_ShouldReturnCardResponse() {
            when(cardDomainService.getCardById(card.getId())).thenReturn(card);

            CardResponse result = applicationService.getCardByIdForUser(card.getId(), user.getUsername());

            assertThat(result).isEqualTo(cardResponse);

            verify(cardDomainService).getCardById(card.getId());
            verify(userDomainService, never()).getByUsername(user.getUsername());
        }

        @Test
        void getCardByIdForUser_WhenUserIsAdmin_ShouldReturnCardResponse() {
            User admin = User.builder().username("admin").role(Role.ADMIN).build();
            when(cardDomainService.getCardById(card.getId())).thenReturn(card);
            when(userDomainService.getByUsername(admin.getUsername())).thenReturn(admin);

            CardResponse result = applicationService.getCardByIdForUser(card.getId(), admin.getUsername());

            assertThat(result).isEqualTo(cardResponse);

            verify(cardDomainService).getCardById(card.getId());
            verify(userDomainService).getByUsername("admin");
        }

        @Test
        void getCardByIdForUser_WhenCardNotFound_ShouldThrowCardNotFoundException() {
            String expectedMessage = "Card with id %d not found".formatted(card.getId());
            when(cardDomainService.getCardById(card.getId())).thenThrow(
                    new CardNotFoundException(expectedMessage)
            );

            assertThatThrownBy(() ->
                    applicationService.getCardByIdForUser(card.getId(), user.getUsername())
            ).isInstanceOf(CardNotFoundException.class)
                    .hasMessageContaining(expectedMessage);

            verify(cardDomainService).getCardById(card.getId());
            verify(userDomainService, never()).getByUsername(user.getUsername());
        }

        @Test
        void getCardByIdForUser_WhenUserIsNotOwnerNorAdmin_ShouldThrowCardAccessDeniedException() {
            when(cardDomainService.getCardById(card.getId())).thenReturn(card);
            when(userDomainService.getByUsername("otherUser")).thenReturn(
                    User.builder().username("otherUser").role(Role.USER).build()
            );

            assertThatThrownBy(() ->
                    applicationService.getCardByIdForUser(card.getId(), "otherUser")
            ).isInstanceOf(CardAccessDeniedException.class)
                    .hasMessageContaining("User 'otherUser' does not have access");

            verify(cardDomainService).getCardById(card.getId());
            verify(userDomainService).getByUsername("otherUser");
        }
    }

    @Nested
    class GetOwnerByCardIdTests {

        @Test
        void getOwnerByCardId_WithValidCardId_ShouldReturnUserResponse() {
            when(cardDomainService.getCardById(card.getId())).thenReturn(card);

            UserResponse result = applicationService.getOwnerByCardId(card.getId());

            assertThat(result).isEqualTo(userResponse);

            verify(cardDomainService).getCardById(card.getId());
        }
    }

    @Nested
    class GetCardsForUserTests {

        @Test
        void getCardsForUser_WhenUserIsRegularUser_ShouldReturnOwnCards() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Card> cardPage = new PageImpl<>(List.of(card));

            when(userDomainService.getByUsername(user.getUsername())).thenReturn(user);
            when(cardDomainService.getCardsByOwner(user, pageable)).thenReturn(cardPage);

            Page<CardResponse> result = applicationService.getCardsForUser(user.getUsername(), pageable);

            assertThat(result.getContent()).containsExactly(cardResponse);

            verify(userDomainService).getByUsername(user.getUsername());
            verify(cardDomainService).getCardsByOwner(user, pageable);
        }

        @Test
        void getCardsForUser_WhenUserIsAdmin_ShouldReturnAllCards() {
            User admin = User.builder().username("admin").role(Role.ADMIN).build();
            Pageable pageable = PageRequest.of(0, 10);
            Page<Card> cardPage = new PageImpl<>(List.of(card));

            when(userDomainService.getByUsername(admin.getUsername())).thenReturn(admin);
            when(cardDomainService.getCards(any(Pageable.class))).thenReturn(cardPage);

            Page<CardResponse> result = applicationService.getCardsForUser(admin.getUsername(), pageable);

            assertThat(result.getContent()).containsExactly(cardResponse);

            verify(userDomainService).getByUsername(admin.getUsername());
            verify(cardDomainService).getCards(pageable);
            verify(cardDomainService, never()).getCardsByOwner(user, pageable);
        }
    }

    @Nested
    class BlockCardByIdTests {

        @Test
        void blockCardById_WhenUserIsOwner_ShouldReturnBlockedCardResponse() {
            card.setCardStatus(CardStatus.BLOCKED);
            when(cardDomainService.getCardById(card.getId())).thenReturn(card);
            when(cardDomainService.blockCard(card)).thenReturn(card);

            CardResponse result = applicationService.blockCardById(card.getId(), TEST_USERNAME);

            assertThat(result.cardStatus()).isEqualTo(CardStatus.BLOCKED);
            verify(cardDomainService).blockCard(card);
        }

        @Test
        void blockCardById_WhenUserIsAdmin_ShouldReturnBlockedCardResponse() {
            card.setCardStatus(CardStatus.BLOCKED);
            User admin = User.builder().username("admin").role(Role.ADMIN).build();

            when(cardDomainService.getCardById(card.getId())).thenReturn(card);
            when(userDomainService.getByUsername(admin.getUsername())).thenReturn(admin);
            when(cardDomainService.blockCard(card)).thenReturn(card);

            CardResponse result = applicationService.blockCardById(card.getId(), admin.getUsername());

            assertThat(result.cardStatus()).isEqualTo(CardStatus.BLOCKED);
        }

        @Test
        void blockCardById_WhenUserIsNotOwnerNorAdmin_ShouldThrowCardOperationNotAllowedException() {
            when(cardDomainService.getCardById(card.getId())).thenReturn(card);
            when(userDomainService.getByUsername("otherUser")).thenReturn(
                    User.builder().username("otherUser").role(Role.USER).build()
            );

            assertThatThrownBy(() ->
                    applicationService.blockCardById(card.getId(), "otherUser")
            ).isInstanceOf(CardOperationNotAllowedException.class)
                    .hasMessageContaining("Access denied to block this card");
        }

        @Test
        void blockCardById_WhenCardIsAlreadyBlocked_ShouldThrowCardOperationNotAllowedException() {
            String expectedMessage = "Cannot block card with status: BLOCKED";
            card.setCardStatus(CardStatus.BLOCKED);
            when(cardDomainService.getCardById(card.getId())).thenReturn(card);
            when(cardDomainService.blockCard(card)).thenThrow(
                    new CardOperationNotAllowedException(expectedMessage)
            );

            assertThatThrownBy(() ->
                    applicationService.blockCardById(card.getId(), TEST_USERNAME)
            ).isInstanceOf(CardOperationNotAllowedException.class)
                    .hasMessageContaining(expectedMessage);
        }
    }

    @Nested
    class ActivateByIdTests {

        @Test
        void activateById_WithValidCardId_ShouldReturnActivatedCardResponse() {
            when(cardDomainService.getCardById(card.getId())).thenReturn(card);
            when(cardDomainService.activateCard(card)).thenReturn(card);

            CardResponse result = applicationService.activateById(card.getId());

            assertThat(result).isEqualTo(cardResponse);
            verify(cardDomainService).activateCard(card);
        }

        @Test
        void activateById_WhenCardIsAlreadyActive_ShouldThrowCardOperationNotAllowedException() {
            when(cardDomainService.getCardById(card.getId())).thenReturn(card);
            when(cardDomainService.activateCard(card)).thenThrow(
                    new CardOperationNotAllowedException("Cannot activate card with status: ACTIVE")
            );

            assertThatThrownBy(() ->
                    applicationService.activateById(card.getId())
            ).isInstanceOf(CardOperationNotAllowedException.class)
                    .hasMessageContaining("Cannot activate card with status: ACTIVE");
        }

        @Test
        void activateById_WhenCardIsExpired_ShouldThrowCardOperationNotAllowedException() {
            String expectedMessage = "Cannot activate expired card";
            card.setCardStatus(CardStatus.EXPIRED);
            when(cardDomainService.getCardById(card.getId())).thenReturn(card);
            when(cardDomainService.activateCard(card)).thenThrow(
                    new CardOperationNotAllowedException(expectedMessage)
            );

            assertThatThrownBy(() ->
                    applicationService.activateById(card.getId())
            ).isInstanceOf(CardOperationNotAllowedException.class)
                    .hasMessageContaining(expectedMessage);
        }
    }

    @Nested
    class DeleteCardByIdTests {

        @Test
        void deleteCardById_WithValidCardId_ShouldDeleteCard() {
            card.setCardStatus(CardStatus.BLOCKED);
            when(cardDomainService.getCardById(card.getId())).thenReturn(card);
            doNothing().when(cardDomainService).deleteCard(card);

            applicationService.deleteCardById(card.getId());

            verify(cardDomainService).deleteCard(card);
        }

        @Test
        void deleteCardById_WhenCardIsActive_ShouldThrowCardOperationNotAllowedException() {
            String expectedMessage = "Cannot delete card with status: ACTIVE. Card must be blocked first";
            when(cardDomainService.getCardById(card.getId())).thenReturn(card);
            doThrow(new CardOperationNotAllowedException(
                    expectedMessage
            )).when(cardDomainService).deleteCard(card);

            assertThatThrownBy(() ->
                    applicationService.deleteCardById(card.getId())
            ).isInstanceOf(CardOperationNotAllowedException.class)
                    .hasMessageContaining(expectedMessage);
        }

        @Test
        void deleteCardById_WhenCardHasNonZeroBalance_ShouldThrowCardOperationNotAllowedException() {
            String expectedMessage = "Cannot delete card with non-zero balance: 100.00";
            when(cardDomainService.getCardById(card.getId())).thenReturn(card);
            doThrow(new CardOperationNotAllowedException(
                    expectedMessage
            )).when(cardDomainService).deleteCard(card);

            assertThatThrownBy(() ->
                    applicationService.deleteCardById(card.getId())
            ).isInstanceOf(CardOperationNotAllowedException.class)
                    .hasMessageContaining(expectedMessage);
        }
    }
}