package com.example.bankcards.controller;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.service.application.TransferApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfers")
@Slf4j
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Transfers", description = "Operations for transferring money between cards")
public class TransferController {

    private final TransferApplicationService applicationService;

    @Operation(
            summary = "Transfer money between user's own cards",
            description = "Only USER can transfer money and only between own cards"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Transfer completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation failed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing, expired or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - user cannot transfer " +
                    "between cards they do not own"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> transfer(
            Authentication authentication,
            @Valid @RequestBody TransferRequest request
    ) {
        String username = authentication.getName();

        log.info("User: {} initiating transfer: fromCardId={} toCardId={} amount={}",
                username, request.fromCardId(), request.toCardId(), request.amount());

        applicationService.transfer(username, request);

        log.info("Transfer completed successfully: user '{}', fromCardId={} toCardId={} amount={}",
                username, request.fromCardId(), request.toCardId(), request.amount());

        return ResponseEntity.noContent().build();
    }
}
