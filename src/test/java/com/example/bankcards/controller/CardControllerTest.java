package com.example.bankcards.controller;

import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.dto.response.UserResponse;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.enums.ErrorCode;
import com.example.bankcards.enums.Role;
import com.example.bankcards.exception.badrequest.CardOperationNotAllowedException;
import com.example.bankcards.exception.conflict.CardAlreadyExistsException;
import com.example.bankcards.exception.forbidden.CardAccessDeniedException;
import com.example.bankcards.exception.notfound.CardNotFoundException;
import com.example.bankcards.exception.notfound.UserNotFoundException;
import com.example.bankcards.service.application.CardApplicationService;
import com.example.bankcards.util.provider.InvalidCardCreateRequestProvider;
import com.example.bankcards.util.provider.InvalidIdProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.example.bankcards.util.TestData.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CardController.class)
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CardApplicationService applicationService;

    private CardCreateRequest cardCreateRequest;
    private CardResponse cardResponse;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        cardCreateRequest = new CardCreateRequest(
                TEST_CARD_NUMBER,
                TEST_USER_ID,
                FUTURE_DATE,
                POSITIVE_BALANCE
        );
        cardResponse = new CardResponse(
                TEST_CARD_ID,
                MASKED_CARD_NUMBER,
                FUTURE_DATE,
                CardStatus.ACTIVE,
                POSITIVE_BALANCE
        );
        userResponse = new UserResponse(
                TEST_USER_ID,
                TEST_USERNAME,
                Role.USER
        );
    }

    @Nested
    class CreateCardTests {

        // --- POSITIVE CASE ---

        @Test
        void createCard_WithValidRequest_ShouldReturnCreated() throws Exception {
            when(applicationService.createCard(cardCreateRequest))
                    .thenReturn(cardResponse);

            mockMvc.perform(post("/api/v1/cards")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cardCreateRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(cardResponse.id()))
                    .andExpect(jsonPath("$.maskedCardNumber").value(cardResponse.maskedCardNumber()))
                    .andExpect(jsonPath("$.expiryDate").value(cardResponse.expiryDate().toString()))
                    .andExpect(jsonPath("$.cardStatus").value(cardResponse.cardStatus().name()))
                    .andExpect(jsonPath("$.balance").value(cardResponse.balance().doubleValue()));

            verify(applicationService).createCard(any(CardCreateRequest.class));
        }

        // -- NEGATIVE CASES ---

        @ParameterizedTest
        @ArgumentsSource(InvalidCardCreateRequestProvider.class)
        void createCard_WithInvalidRequest_ShouldReturnBadRequest(CardCreateRequest request) throws Exception {
            mockMvc.perform(post("/api/v1/cards")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(applicationService, never()).createCard(any());
        }

        @Test
        void createCard_WhenOwnerNotFound_ShouldReturnNotFound() throws Exception {
            String expectedMessage = "Owner not found";

            when(applicationService.createCard(cardCreateRequest))
                    .thenThrow(new UserNotFoundException(expectedMessage));

            mockMvc.perform(post("/api/v1/cards")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cardCreateRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(expectedMessage))
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.NOT_FOUND.name()));

            verify(applicationService).createCard(any(CardCreateRequest.class));
        }

        @Test
        void createCard_WhenCardNumberAlreadyExists_ShouldReturnConflict() throws Exception {
            String expectedMessage = "Card with number '%s' already exists".formatted(cardResponse.maskedCardNumber());

            when(applicationService.createCard(cardCreateRequest))
                    .thenThrow(new CardAlreadyExistsException(expectedMessage));

            mockMvc.perform(post("/api/v1/cards")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cardCreateRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(expectedMessage))
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.CONFLICT.name()));

            verify(applicationService).createCard(any(CardCreateRequest.class));
        }
    }


    @Nested
    class GetCardTests {

        // --- POSITIVE CASE ---

        @Test
        void getCard_WithValidId_ShouldReturnOk() throws Exception {
            when(applicationService.getCardByIdForUser(TEST_CARD_ID, TEST_USERNAME))
                    .thenReturn(cardResponse);

            mockMvc.perform(get("/api/v1/cards/{id}", TEST_CARD_ID)
                            .with(jwt().jwt(jwt -> jwt.subject(TEST_USERNAME))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(TEST_CARD_ID))
                    .andExpect(jsonPath("$.maskedCardNumber").value(MASKED_CARD_NUMBER));

            verify(applicationService).getCardByIdForUser(TEST_CARD_ID, TEST_USERNAME);
        }

        // --- NEGATIVE CASES ---

        @ParameterizedTest
        @ArgumentsSource(InvalidIdProvider.class)
        void getCard_WithInvalidId_ShouldReturnBadRequest(Long cardId) throws Exception {
            mockMvc.perform(get("/api/v1/cards/{id}", cardId)
                            .with(jwt().jwt(jwt -> jwt.subject(TEST_USERNAME))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.BAD_REQUEST.name()));

            verify(applicationService, never()).getCardByIdForUser(anyLong(), anyString());
        }

        @Test
        void getCard_WhenCardNotFound_ShouldReturnNotFound() throws Exception {
            String expectedMessage = "Card not found";
            when(applicationService.getCardByIdForUser(TEST_CARD_ID, TEST_USERNAME))
                    .thenThrow(new CardNotFoundException(expectedMessage));

            mockMvc.perform(get("/api/v1/cards/{id}", TEST_CARD_ID)
                            .with(jwt().jwt(jwt -> jwt.subject(TEST_USERNAME))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(expectedMessage))
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.NOT_FOUND.name()));

            verify(applicationService).getCardByIdForUser(TEST_CARD_ID, TEST_USERNAME);
        }

        @Test
        void getCard_WhenUserNotOwner_ShouldReturnForbidden() throws Exception {
            String expectedMessage = "Access denied";
            when(applicationService.getCardByIdForUser(anyLong(), anyString()))
                    .thenThrow(new CardAccessDeniedException(expectedMessage));

            mockMvc.perform(get("/api/v1/cards/{id}", TEST_CARD_ID)
                            .with(jwt().jwt(jwt -> jwt.subject(TEST_USERNAME))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(expectedMessage))
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.FORBIDDEN.name()));

            verify(applicationService).getCardByIdForUser(TEST_CARD_ID, TEST_USERNAME);
        }
    }


    @Nested
    class GetCardOwnerTests {

        // --- POSITIVE CASE ---

        @Test
        void getCardOwner_WithValidCardId_ShouldReturnOk() throws Exception {
            when(applicationService.getOwnerByCardId(TEST_CARD_ID))
                    .thenReturn(userResponse);

            mockMvc.perform(get("/api/v1/cards/{id}/owner", TEST_CARD_ID)
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userResponse.id()))
                    .andExpect(jsonPath("$.username").value(userResponse.username()))
                    .andExpect(jsonPath("$.role").value(userResponse.role().name()));

            verify(applicationService).getOwnerByCardId(TEST_CARD_ID);
        }

        // --- NEGATIVE CASES ---

        @ParameterizedTest
        @ArgumentsSource(InvalidIdProvider.class)
        void getCardOwner_WithInvalidCardId_ShouldReturnBadRequest(Long cardId) throws Exception {
            mockMvc.perform(get("/api/v1/cards/{id}/owner", cardId)
                            .with(jwt()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.BAD_REQUEST.name()));
        }

        @Test
        void getCardOwner_WhenCardNotFound_ShouldReturnNotFound() throws Exception {
            String expectedMessage = "Card not found";
            when(applicationService.getOwnerByCardId(TEST_CARD_ID))
                    .thenThrow(new CardNotFoundException(expectedMessage));

            mockMvc.perform(get("/api/v1/cards/{id}/owner", TEST_CARD_ID)
                            .with(jwt().jwt(jwt -> jwt.subject(TEST_USERNAME))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(expectedMessage))
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.NOT_FOUND.name()));

            verify(applicationService).getOwnerByCardId(TEST_CARD_ID);
        }
    }


    @Nested
    class GetCardsTests {

        // --- POSITIVE CASE ---

        @Test
        void getCards_ShouldReturnPaginated() throws Exception {
            Page<CardResponse> page = new PageImpl<>(List.of(cardResponse));

            when(applicationService.getCardsForUser(eq(TEST_USERNAME), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/v1/cards")
                            .param("page", "0")
                            .param("size", "10")
                            .with(jwt().jwt(jwt -> jwt.subject(TEST_USERNAME))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(TEST_CARD_ID));

            verify(applicationService).getCardsForUser(eq(TEST_USERNAME), argThat(pageable ->
                    pageable.getPageNumber() == 0 && pageable.getPageSize() == 10
            ));
        }
    }


    @Nested
    class BlockCardTests {

        // --- POSITIVE CASE ---

        @Test
        void blockCard_WithValidId_ShouldReturnOk() throws Exception {
            CardResponse cardResponse = new CardResponse(
                    TEST_CARD_ID, MASKED_CARD_NUMBER, FUTURE_DATE, CardStatus.BLOCKED, POSITIVE_BALANCE
            );

            when(applicationService.blockCardById(TEST_CARD_ID, TEST_USERNAME))
                    .thenReturn(cardResponse);

            mockMvc.perform(patch("/api/v1/cards/{id}/block", TEST_CARD_ID)
                            .with(jwt().jwt(jwt -> jwt.subject(TEST_USERNAME))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(TEST_CARD_ID))
                    .andExpect(jsonPath("$.cardStatus").value(CardStatus.BLOCKED.name()));

            verify(applicationService).blockCardById(TEST_CARD_ID, TEST_USERNAME);
        }

        // --- NEGATIVE CASES ---

        @ParameterizedTest
        @ArgumentsSource(InvalidIdProvider.class)
        void blockCard_WithInvalidId_ShouldReturnBadRequest(Long id) throws Exception {
            mockMvc.perform(patch("/api/v1/cards/{id}/block", id)
                            .with(jwt().jwt(jwt -> jwt.subject(TEST_USERNAME))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.BAD_REQUEST.name()));

            verify(applicationService, never()).blockCardById(anyLong(), anyString());
        }

        @Test
        void blockCard_WhenCardNotFound_ShouldReturnNotFound() throws Exception {
            String expectedMessage = "Card not found";
            when(applicationService.blockCardById(TEST_CARD_ID, TEST_USERNAME))
                    .thenThrow(new CardNotFoundException(expectedMessage));

            mockMvc.perform(patch("/api/v1/cards/{id}/block", TEST_CARD_ID)
                            .with(jwt().jwt(jwt -> jwt.subject(TEST_USERNAME))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(expectedMessage))
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.NOT_FOUND.name()));

            verify(applicationService).blockCardById(TEST_CARD_ID, TEST_USERNAME);
        }

        @Test
        void blockCard_WhenAlreadyBlocked_ShouldReturnBadRequest() throws Exception {
            String expectedMessage = "Card is already blocked";
            when(applicationService.blockCardById(TEST_CARD_ID, TEST_USERNAME))
                    .thenThrow(new CardOperationNotAllowedException(expectedMessage));

            mockMvc.perform(patch("/api/v1/cards/{id}/block", TEST_CARD_ID)
                            .with(jwt().jwt(jwt -> jwt.subject(TEST_USERNAME))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(expectedMessage))
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.BAD_REQUEST.name()));

            verify(applicationService).blockCardById(TEST_CARD_ID, TEST_USERNAME);
        }

        @Test
        void blockCard_WhenExpired_ShouldReturnBadRequest() throws Exception {
            String expectedMessage = "Card is expired and cannot be blocked";
            when(applicationService.blockCardById(TEST_CARD_ID, TEST_USERNAME))
                    .thenThrow(new CardOperationNotAllowedException(expectedMessage));

            mockMvc.perform(patch("/api/v1/cards/{id}/block", TEST_CARD_ID)
                            .with(jwt().jwt(jwt -> jwt.subject(TEST_USERNAME))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(expectedMessage))
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.BAD_REQUEST.name()));

            verify(applicationService).blockCardById(TEST_CARD_ID, TEST_USERNAME);
        }

        @Test
        void blockCard_WhenUserNotOwner_ShouldReturnForbidden() throws Exception {
            String expectedMessage = "Access denied";
            when(applicationService.blockCardById(TEST_CARD_ID, TEST_USERNAME))
                    .thenThrow(new CardAccessDeniedException(expectedMessage));

            mockMvc.perform(patch("/api/v1/cards/{id}/block", TEST_CARD_ID)
                            .with(jwt().jwt(jwt -> jwt.subject(TEST_USERNAME))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(expectedMessage))
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.FORBIDDEN.name()));

            verify(applicationService).blockCardById(TEST_CARD_ID, TEST_USERNAME);
        }
    }


    @Nested
    class ActivateCardTests {

        // --- POSITIVE CASE ---

        @Test
        void activateCard_WithValidId_ShouldReturnOk() throws Exception {
            when(applicationService.activateById(anyLong()))
                    .thenReturn(cardResponse);

            mockMvc.perform(patch("/api/v1/cards/{id}/activate", TEST_CARD_ID)
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(TEST_CARD_ID))
                    .andExpect(jsonPath("$.cardStatus").value(CardStatus.ACTIVE.name()));

            verify(applicationService).activateById(TEST_CARD_ID);
        }

        // --- NEGATIVE CASES ---

        @ParameterizedTest
        @ArgumentsSource(InvalidIdProvider.class)
        void activateCard_WithInvalidId_ShouldReturnBadRequest(Long id) throws Exception {
            mockMvc.perform(patch("/api/v1/cards/{id}/activate", id)
                            .with(jwt()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.BAD_REQUEST.name()));

            verify(applicationService, never()).activateById(anyLong());
        }

        @Test
        void activateCard_WhenCardNotFound_ShouldReturnNotFound() throws Exception {
            String expectedMessage = "Card not found";
            when(applicationService.activateById(TEST_CARD_ID))
                    .thenThrow(new CardNotFoundException(expectedMessage));

            mockMvc.perform(patch("/api/v1/cards/{id}/activate", TEST_CARD_ID)
                            .with(jwt()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(expectedMessage))
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.NOT_FOUND.name()));

            verify(applicationService).activateById(TEST_CARD_ID);
        }

        @Test
        void activateCard_WhenAlreadyActive_ShouldReturnBadRequest() throws Exception {
            String expectedMessage = "Card is already active";
            when(applicationService.activateById(TEST_CARD_ID))
                    .thenThrow(new CardOperationNotAllowedException(expectedMessage));

            mockMvc.perform(patch("/api/v1/cards/{id}/activate", TEST_CARD_ID)
                            .with(jwt()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(expectedMessage))
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.BAD_REQUEST.name()));

            verify(applicationService).activateById(TEST_CARD_ID);
        }

        @Test
        void activateCard_WhenExpired_ShouldReturnBadRequest() throws Exception {
            String expectedMessage = "Cannot activate expired card";
            when(applicationService.activateById(TEST_CARD_ID))
                    .thenThrow(new CardOperationNotAllowedException(expectedMessage));

            mockMvc.perform(patch("/api/v1/cards/{id}/activate", TEST_CARD_ID)
                            .with(jwt()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(expectedMessage))
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.BAD_REQUEST.name()));

            verify(applicationService).activateById(TEST_CARD_ID);
        }
    }

    @Nested
    class DeleteCardTests {

        // --- POSITIVE CASE ---

        @Test
        void deleteCard_WithValidId_ShouldReturnNoContent() throws Exception {
            mockMvc.perform(delete("/api/v1/cards/{id}", TEST_CARD_ID)
                            .with(jwt()))
                    .andExpect(status().isNoContent());

            verify(applicationService).deleteCardById(TEST_CARD_ID);
        }

        // --- NEGATIVE CASES

        @ParameterizedTest
        @ArgumentsSource(InvalidIdProvider.class)
        void deleteCard_WithInvalidId_ShouldReturnBadRequest(Long id) throws Exception {
            mockMvc.perform(delete("/api/v1/cards/{id}", id)
                            .with(jwt()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.BAD_REQUEST.name()));

            verify(applicationService, never()).deleteCardById(TEST_CARD_ID);
        }

        @Test
        void deleteCard_WhenCardNotFound_ShouldReturnNotFound() throws Exception {
            String expectedMessage = "Card not found";
            doThrow(new CardNotFoundException(expectedMessage))
                    .when(applicationService)
                    .deleteCardById(TEST_CARD_ID);

            mockMvc.perform(delete("/api/v1/cards/{id}", TEST_CARD_ID)
                            .with(jwt()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(expectedMessage))
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.NOT_FOUND.name()));

            verify(applicationService).deleteCardById(TEST_CARD_ID);
        }

        @Test
        void deleteCard_WhenActive_ShouldReturnBadRequest() throws Exception {
            String expectedMessage = "Cannot delete ACTIVE card. Card must be blocked first";
            doThrow(new CardOperationNotAllowedException(expectedMessage))
                    .when(applicationService)
                    .deleteCardById(TEST_CARD_ID);

            mockMvc.perform(delete("/api/v1/cards/{id}", TEST_CARD_ID)
                            .with(jwt()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(expectedMessage))
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.BAD_REQUEST.name()));

            verify(applicationService).deleteCardById(TEST_CARD_ID);
        }

        @Test
        void deleteCard_WithNonZeroBalance_ShouldReturnBadRequest() throws Exception {
            String expectedMessage = "Cannot delete card with non-zero balance";
            doThrow(new CardOperationNotAllowedException(expectedMessage))
                    .when(applicationService)
                    .deleteCardById(TEST_CARD_ID);

            mockMvc.perform(delete("/api/v1/cards/{id}", TEST_CARD_ID)
                            .with(jwt()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(expectedMessage))
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.BAD_REQUEST.name()));

            verify(applicationService).deleteCardById(TEST_CARD_ID);
        }
    }
}