package com.example.bankcards.controller;

import com.example.bankcards.dto.request.UserCreateRequest;
import com.example.bankcards.dto.request.UserPasswordUpdateRequest;
import com.example.bankcards.dto.request.UserUpdateRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.dto.response.UserResponse;
import com.example.bankcards.service.application.UserApplicationService;
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
@Validated
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Users", description = "Endpoints for user management operations")
public class UserController {

    private final UserApplicationService applicationService;

    @Operation(summary = "Create a new user", description = "Only ADMIN can create new users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing, expired, or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden: insufficient privileges"),
            @ApiResponse(responseCode = "409", description = "Conflict - username already exists")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody UserCreateRequest request
    ) {
        log.info("Admin creating new user with username '{}'", request.username());
        UserResponse response = applicationService.createUser(request);
        log.info("User '{}' created successfully with ID {}", response.username(), response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get user by ID", description = "Only ADMIN can retrieve any user by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid user ID"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing, expired, or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden: insufficient privileges"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUser(
            @ValidId @PathVariable Long id
    ) {
        log.info("Admin retrieving user with ID {}", id);
        UserResponse response = applicationService.getUserById(id);
        log.info("User with ID {} retrieved successfully by admin", id);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get current authenticated user",
            description = "Authenticated users (ADMIN or USER) can retrieve their own information"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Current user retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing, expired, or invalid")
    })
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        log.info("User '{}' retrieving own information", username);
        UserResponse response = applicationService.getUserByUsername(username);
        log.info("Current user '{}' retrieved successfully", username);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get paginated list of users", description = "Only ADMIN can retrieve all users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing, expired, or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden: insufficient privileges")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getUsers(Pageable pageable) {
        log.info("Admin retrieving paginated list of users: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());
        Page<UserResponse> response = applicationService.getUsers(pageable);
        log.info("Admin retrieved {} users", response.getNumberOfElements());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get current user's cards", description = "USER can retrieve only their own cards")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cards retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing, expired, or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden: insufficient privileges")
    })
    @GetMapping("/me/cards")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<CardResponse>> getUserOwnCards(
            Authentication authentication,
            Pageable pageable
    ) {
        String username = authentication.getName();
        log.info("User '{}' retrieving own cards: page={}, size={}",
                username, pageable.getPageNumber(), pageable.getPageSize());
        Page<CardResponse> response = applicationService.getCardsForUser(username, pageable);
        log.info("User '{}' retrieved {} cards", username, response.getNumberOfElements());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update user by ID", description = "Only ADMIN can update any user's details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing, expired, or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden: insufficient privileges"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "Conflict - username already exists")
    })
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUser(
            @ValidId @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        log.info("Admin updating user with ID {}", id);
        UserResponse response = applicationService.updateUser(id, request);
        log.info("User with ID {} updated successfully by admin", id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update user password", description = "Only ADMIN can update any user's password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Password updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing, expired, or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden: insufficient privileges"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updatePassword(
            @ValidId @PathVariable Long id,
            @Valid @RequestBody UserPasswordUpdateRequest request
    ) {
        log.info("Admin updating password for user with ID {}", id);
        applicationService.updateUserPassword(id, request);
        log.info("Password updated successfully for user ID {} by admin", id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete user by ID", description = "Only ADMIN can delete a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid user ID"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing, expired, or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden: insufficient privileges"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @ValidId @PathVariable Long id
    ) {
        log.info("Admin deleting user with ID {}", id);
        applicationService.deleteUserById(id);
        log.info("User with ID {} deleted successfully by admin", id);
        return ResponseEntity.noContent().build();
    }
}
