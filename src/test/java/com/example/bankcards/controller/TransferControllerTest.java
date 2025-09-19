package com.example.bankcards.controller;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.enums.ErrorCode;
import com.example.bankcards.exception.forbidden.CardAccessDeniedException;
import com.example.bankcards.exception.badrequest.InsufficientFundsException;
import com.example.bankcards.exception.badrequest.TransferOperationNotAllowedException;
import com.example.bankcards.exception.notfound.CardNotFoundException;
import com.example.bankcards.service.application.TransferApplicationService;
import com.example.bankcards.util.provider.InvalidAmountProvider;
import com.example.bankcards.util.provider.InvalidIdProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static com.example.bankcards.util.TestData.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransferController.class)
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransferApplicationService applicationService;

    private TransferRequest transferRequest;

    @BeforeEach
    void setUp() {
        transferRequest = new TransferRequest(
                TEST_CARD_ID,
                TEST_CARD_ID_2,
                new BigDecimal("10.00")
        );
    }

    @Nested
    class TransferTests {

        // --- POSITIVE CASE ---

        @Test
        void transfer_WithValidRequest_ShouldReturnNoContent() throws Exception {
            mockMvc.perform(post("/api/v1/transfers")
                            .with(jwt().jwt(jwt -> jwt.subject(TEST_USERNAME)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(transferRequest)))
                    .andExpect(status().isNoContent());

            verify(applicationService).transfer(eq(TEST_USERNAME), any(TransferRequest.class));
        }

        // --- NEGATIVE CASES ---

        @ParameterizedTest
        @ArgumentsSource(InvalidIdProvider.class)
        void transfer_WithInvalidFromCardId_ShouldReturnBadRequest(Long fromCardId) throws Exception {
            TransferRequest request = new TransferRequest(
                    fromCardId,
                    TEST_CARD_ID_2,
                    new BigDecimal("10.00")
            );

            mockMvc.perform(post("/api/v1/transfers")
                            .with(jwt().jwt(jwt -> jwt.subject(TEST_USERNAME)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.BAD_REQUEST.name()));

            verify(applicationService, never()).transfer(anyString(), any(TransferRequest.class));
        }

        @ParameterizedTest
        @ArgumentsSource(InvalidIdProvider.class)
        void transfer_WithInvalidToCardId_ShouldReturnBadRequest(Long toCardId) throws Exception {
            TransferRequest request = new TransferRequest(
                    TEST_CARD_ID,
                    toCardId,
                    new BigDecimal("10.00")
            );

            mockMvc.perform(post("/api/v1/transfers")
                            .with(jwt().jwt(jwt -> jwt.subject(TEST_USERNAME)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.BAD_REQUEST.name()));

            verify(applicationService, never()).transfer(anyString(), any(TransferRequest.class));
        }

        @ParameterizedTest
        @ArgumentsSource(InvalidAmountProvider.class)
        void transfer_WithInvalidAmount_ShouldReturnBadRequest(BigDecimal amount) throws Exception {
            TransferRequest request = new TransferRequest(
                    TEST_CARD_ID,
                    TEST_CARD_ID_2,
                    amount
            );

            mockMvc.perform(post("/api/v1/transfers")
                            .with(jwt().jwt(jwt -> jwt.subject(TEST_USERNAME)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.BAD_REQUEST.name()));

            verify(applicationService, never()).transfer(anyString(), any(TransferRequest.class));
        }

        @Test
        void transfer_WhenCardNotFound_ShouldReturnNotFound() throws Exception {
            String expectedMessage = "Card not found";

            doThrow(new CardNotFoundException(expectedMessage))
                    .when(applicationService)
                    .transfer(eq(TEST_USERNAME), any(TransferRequest.class));

            mockMvc.perform(post("/api/v1/transfers")
                            .with(jwt().jwt(jwt -> jwt.subject(TEST_USERNAME)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(transferRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(expectedMessage))
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.NOT_FOUND.name()));

            verify(applicationService).transfer(eq(TEST_USERNAME), any(TransferRequest.class));
        }

        @Test
        void transfer_WhenUserNotOwner_ShouldReturnForbidden() throws Exception {
            String expectedMessage = "Access denied to card";

            doThrow(new CardAccessDeniedException(expectedMessage))
                    .when(applicationService)
                    .transfer(eq(TEST_USERNAME), any(TransferRequest.class));

            mockMvc.perform(post("/api/v1/transfers")
                            .with(jwt().jwt(jwt -> jwt.subject(TEST_USERNAME)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(transferRequest)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(expectedMessage))
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.FORBIDDEN.name()));

            verify(applicationService).transfer(eq(TEST_USERNAME), any(TransferRequest.class));
        }

        @Test
        void transfer_WhenInsufficientFunds_ShouldReturnBadRequest() throws Exception {
            TransferRequest request = new TransferRequest(
                    TEST_CARD_ID,
                    TEST_CARD_ID_2,
                    new BigDecimal("1000.00")
            );
            String expectedMessage = "Insufficient funds";

            doThrow(new InsufficientFundsException(expectedMessage))
                    .when(applicationService)
                    .transfer(eq(TEST_USERNAME), any(TransferRequest.class));

            mockMvc.perform(post("/api/v1/transfers")
                            .with(jwt().jwt(jwt -> jwt.subject(TEST_USERNAME)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(expectedMessage))
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.BAD_REQUEST.name()));

            verify(applicationService).transfer(eq(TEST_USERNAME), any(TransferRequest.class));
        }

        @Test
        void transfer_WhenSameCard_ShouldReturnBadRequest() throws Exception {
            TransferRequest request = new TransferRequest(
                    TEST_CARD_ID,
                    TEST_CARD_ID,
                    new BigDecimal("100.00")
            );
            String expectedMessage = "Cannot transfer to the same card";

            doThrow(new TransferOperationNotAllowedException(expectedMessage))
                    .when(applicationService)
                    .transfer(eq(TEST_USERNAME), any(TransferRequest.class));

            mockMvc.perform(post("/api/v1/transfers")
                            .with(jwt().jwt(jwt -> jwt.subject(TEST_USERNAME)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(expectedMessage))
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.BAD_REQUEST.name()));

            verify(applicationService).transfer(eq(TEST_USERNAME), any(TransferRequest.class));
        }
    }
}