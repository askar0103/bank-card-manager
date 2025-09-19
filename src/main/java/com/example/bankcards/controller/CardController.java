package com.example.bankcards.controller;

import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.dto.response.UserResponse;
import com.example.bankcards.service.application.CardApplicationService;
import com.example.bankcards.util.CardNumberMasker;
import com.example.bankcards.validation.annotation.ValidId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cards")
@Validated
@Slf4j
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cards", description = "Endpoints for card operations")
public class CardController {

    private final CardApplicationService applicationService;

    @Operation(summary = "Create a new card", description = "Only ADMIN can create cards")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Card created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing, expired or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden: insufficient privileges"),
            @ApiResponse(responseCode = "404", description = "Owner not found"),
            @ApiResponse(responseCode = "409", description = "Conflict - card number already exists")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardResponse> createCard(
            @Valid @RequestBody CardCreateRequest request
    ) {
        log.info("Admin creating new card with number: {}", CardNumberMasker.mask(request.cardNumber()));
        CardResponse response = applicationService.createCard(request);
        log.info("Card '{}' created successfully for owner ID {}", response.maskedCardNumber(), request.ownerId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Get a card by ID",
            description = "USER can retrieve only their own card. ADMIN can retrieve any card by ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid card ID"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing, expired or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden: insufficient privileges " +
                    "or trying to access another user's card"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<CardResponse> getCard(
            Authentication authentication,
            @ValidId @PathVariable Long id
    ) {
        String username = authentication.getName();
        log.info("User '{}' retrieving card with ID {}", username, id);
        CardResponse response = applicationService.getCardByIdForUser(id, username);
        log.info("Card with ID {} retrieved successfully for user '{}'", id, username);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get the owner of a card by ID",
            description = "ADMIN can retrieve the owner information of any card by its ID. " +
                    "Returns details of the user who owns the card."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Owner retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid card ID"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing, expired or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden: insufficient privileges"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @GetMapping("/{id}/owner")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getCardOwner(
            @ValidId @PathVariable Long id
    ) {
        log.info("Admin retrieving owner for card ID: {}", id);
        UserResponse response = applicationService.getOwnerByCardId(id);
        log.info("Owner '{}' retrieved successfully for card ID {}", response.username(), id);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get cards",
            description = "USER can retrieve only their own cards. " +
                    "ADMIN can retrieve all cards. " +
                    "Supports pagination and sorting."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cards retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing, expired or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden: insufficient privileges")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Page<CardResponse>> getCards(
            Authentication authentication,
            Pageable pageable
    ) {
        String username = authentication.getName();
        log.info("User '{}' retrieving cards with pagination: page={}, size={}",
                username, pageable.getPageNumber(), pageable.getPageSize());
        Page<CardResponse> response = applicationService.getCardsForUser(username, pageable);
        log.info("User '{}' retrieved {} cards", username, response.getNumberOfElements());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Block a card by ID",
            description = "USER can block only their own cards. ADMIN can block any card."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card blocked successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid card ID"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing, expired or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden: insufficient privileges " +
                    "or trying to block another user's card"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PatchMapping("/{id}/block")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<CardResponse> blockCard(
            Authentication authentication,
            @ValidId @PathVariable Long id
    ) {
        String username = authentication.getName();
        log.info("User '{}' attempting to block card ID {}", username, id);
        CardResponse response = applicationService.blockCardById(id, username);
        log.info("Card ID {} blocked successfully by user '{}'", id, username);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Activate  a card by ID",
            description = "Only ADMIN can activate a card"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card activated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid card ID"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing, expired or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden: insufficient privileges"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardResponse> activateCard(
            @ValidId @PathVariable Long id
    ) {
        log.info("Admin activating card with ID {}", id);
        CardResponse response = applicationService.activateById(id);
        log.info("Card ID {} activated successfully", id);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete a card by ID",
            description = "Only ADMIN can delete a card"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Card deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid card ID"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing, expired or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden: insufficient privileges"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCard(
            @ValidId @PathVariable Long id
    ) {
        log.info("Admin deleting card with ID {}", id);
        applicationService.deleteCardById(id);
        log.info("Card ID {} deleted successfully", id);
        return ResponseEntity.noContent().build();
    }
}
