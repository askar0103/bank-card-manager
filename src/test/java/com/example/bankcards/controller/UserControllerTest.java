package com.example.bankcards.controller;

import com.example.bankcards.dto.request.UserCreateRequest;
import com.example.bankcards.dto.request.UserPasswordUpdateRequest;
import com.example.bankcards.dto.request.UserUpdateRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.dto.response.UserResponse;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.enums.ErrorCode;
import com.example.bankcards.enums.Role;
import com.example.bankcards.exception.conflict.UserAlreadyExistsException;
import com.example.bankcards.exception.notfound.UserNotFoundException;
import com.example.bankcards.service.application.UserApplicationService;
import com.example.bankcards.util.provider.InvalidIdProvider;
import com.example.bankcards.util.provider.InvalidPasswordProvider;
import com.example.bankcards.util.provider.InvalidUsernamePasswordProvider;
import com.example.bankcards.util.provider.InvalidUsernameProvider;
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
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserApplicationService applicationService;

    private UserCreateRequest userCreateRequest;
    private UserPasswordUpdateRequest userPasswordUpdateRequest;
    private UserUpdateRequest userUpdateRequest;
    private CardResponse cardResponse;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        userCreateRequest = new UserCreateRequest(
                TEST_USERNAME,
                TEST_USER_PASSWORD,
                Role.USER
        );
        userPasswordUpdateRequest = new UserPasswordUpdateRequest(
                NEW_USER_PASSWORD
        );
        userUpdateRequest = new UserUpdateRequest(
                NEW_USERNAME
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
    class CreateUserTests {

        // --- POSITIVE CASE ---

        @Test
        void createUser_WithValidRequest_ShouldReturnCreated() throws Exception {
            when(applicationService.createUser(userCreateRequest))
                    .thenReturn(userResponse);

            mockMvc.perform(post("/api/v1/users")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userCreateRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(userResponse.id()))
                    .andExpect(jsonPath("$.username").value(userCreateRequest.username()))
                    .andExpect(jsonPath("$.role").value(Role.USER.name()));

            verify(applicationService).createUser(userCreateRequest);
        }

        // --- NEGATIVE CASES ---

        @ParameterizedTest
        @ArgumentsSource(InvalidUsernamePasswordProvider.class)
        void createUser_WithInvalidRequest_ShouldReturnBadRequest(String username, String password) throws Exception {
            UserCreateRequest request = new UserCreateRequest(username, password, Role.USER);

            mockMvc.perform(post("/api/v1/users")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.BAD_REQUEST.name()));

            verify(applicationService, never()).createUser(request);
        }

        @Test
        void createUser_WhenUsernameAlreadyExists_ShouldReturnConflict() throws Exception {
            String expectedMessage = "User '%s' already exists".formatted(userCreateRequest.username());

            when(applicationService.createUser(userCreateRequest))
                    .thenThrow(new UserAlreadyExistsException(expectedMessage));

            mockMvc.perform(post("/api/v1/users")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userCreateRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(expectedMessage))
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.CONFLICT.name()));

            verify(applicationService).createUser(userCreateRequest);
        }
    }


    @Nested
    class GetUserTests {

        // --- POSITIVE CASE ---

        @Test
        void getUser_WithValidId_ShouldReturnOk() throws Exception {
            when(applicationService.getUserById(TEST_USER_ID))
                    .thenReturn(userResponse);

            mockMvc.perform(get("/api/v1/users/{id}", TEST_USER_ID)
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(TEST_USER_ID))
                    .andExpect(jsonPath("$.username").value(TEST_USERNAME));

            verify(applicationService).getUserById(TEST_USER_ID);
        }

        // --- NEGATIVE CASES ---

        @ParameterizedTest
        @ArgumentsSource(InvalidIdProvider.class)
        void getUser_WithInvalidId_ShouldReturnBadRequest(Long id) throws Exception {
            mockMvc.perform(get("/api/v1/users/{id}", id)
                            .with(jwt()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.BAD_REQUEST.name()));

            verify(applicationService, never()).getUserById(anyLong());
        }

        @Test
        void getUser_WhenUserNotFound_ShouldReturnNotFound() throws Exception {
            String expectedMessage = "User not found";

            when(applicationService.getUserById(TEST_USER_ID))
                    .thenThrow(new UserNotFoundException(expectedMessage));

            mockMvc.perform(get("/api/v1/users/{id}", TEST_USER_ID)
                            .with(jwt()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(expectedMessage))
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.NOT_FOUND.name()));

            verify(applicationService).getUserById(TEST_USER_ID);
        }
    }


    @Nested
    class GetCurrentUserTests {

        // --- POSITIVE CASE ---

        @Test
        void getCurrentUser_ShouldReturnOk() throws Exception {
            when(applicationService.getUserByUsername(TEST_USERNAME))
                    .thenReturn(userResponse);

            mockMvc.perform(get("/api/v1/users/me")
                            .with(jwt().jwt(jwt -> jwt.subject(TEST_USERNAME))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userResponse.id()))
                    .andExpect(jsonPath("$.username").value(userResponse.username()))
                    .andExpect(jsonPath("$.role").value(userResponse.role().name()));

            verify(applicationService).getUserByUsername(TEST_USERNAME);
        }
    }

    @Nested
    class GetUsersTests {

        // --- POSITIVE CASE ---

        @Test
        void getUsers_ShouldReturnPaginated() throws Exception {
            Page<UserResponse> page = new PageImpl<>(List.of(userResponse));

            when(applicationService.getUsers(any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/v1/users")
                            .param("page", "0")
                            .param("size", "10")
                            .param("sort", "username,desc")
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(userResponse.id()))
                    .andExpect(jsonPath("$.content[0].username").value(userResponse.username()))
                    .andExpect(jsonPath("$.content[0].role").value(userResponse.role().name()));

            verify(applicationService).getUsers(argThat(pageable ->
                    pageable.getPageNumber() == 0 &&
                            pageable.getPageSize() == 10 &&
                            pageable.getSort().toString().contains("username: DESC")
            ));
        }
    }

    @Nested
    class GetUserOwnCardsTests {

        // --- POSITIVE CASE ---

        @Test
        void getUserOwnCards_ShouldReturnPaginated() throws Exception {
            Page<CardResponse> page = new PageImpl<>(List.of(cardResponse));

            when(applicationService.getCardsForUser(eq(TEST_USERNAME), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/v1/users/me/cards")
                            .param("page", "0")
                            .param("size", "10")
                            .with(jwt().jwt(jwt -> jwt.subject(TEST_USERNAME))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(cardResponse.id()))
                    .andExpect(jsonPath("$.content[0].maskedCardNumber").value(cardResponse.maskedCardNumber()));

            verify(applicationService).getCardsForUser(eq(TEST_USERNAME), argThat(pageable ->
                    pageable.getPageNumber() == 0 && pageable.getPageSize() == 10
            ));
        }
    }

    @Nested
    class UpdateUserTests {

        // --- POSITIVE CASE ---

        @Test
        void updateUser_WithValidRequest_ShouldReturnOk() throws Exception {
            when(applicationService.updateUser(eq(TEST_USER_ID), any(UserUpdateRequest.class)))
                    .thenReturn(userResponse);

            mockMvc.perform(patch("/api/v1/users/{id}", TEST_USER_ID)
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userUpdateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userResponse.id()));

            verify(applicationService).updateUser(eq(TEST_USER_ID), any(UserUpdateRequest.class));
        }

        // --- NEGATIVE CASES ---

        @ParameterizedTest
        @ArgumentsSource(InvalidUsernameProvider.class)
        void updateUser_WithInvalidRequest_ShouldReturnBadRequest(String newUsername) throws Exception {
            UserUpdateRequest request = new UserUpdateRequest(newUsername);

            mockMvc.perform(patch("/api/v1/users/{id}", TEST_USER_ID)
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.BAD_REQUEST.name()));

            verify(applicationService, never()).updateUser(anyLong(), any());
        }

        @Test
        void updateUser_WhenUserNotFound_ShouldReturnNotFound() throws Exception {
            String expectedMessage = "User not found";

            when(applicationService.updateUser(eq(TEST_USER_ID), any(UserUpdateRequest.class)))
                    .thenThrow(new UserNotFoundException(expectedMessage));

            mockMvc.perform(patch("/api/v1/users/{id}", TEST_USER_ID)
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userUpdateRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(expectedMessage))
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.NOT_FOUND.name()));

            verify(applicationService).updateUser(eq(TEST_USER_ID), any(UserUpdateRequest.class));
        }

        @Test
        void updateUser_WhenUsernameAlreadyExists_ShouldReturnConflict() throws Exception {
            String expectedMessage = "Username already exists";

            when(applicationService.updateUser(eq(TEST_USER_ID), any(UserUpdateRequest.class)))
                    .thenThrow(new UserAlreadyExistsException(expectedMessage));

            mockMvc.perform(patch("/api/v1/users/{id}", TEST_USER_ID)
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userUpdateRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(expectedMessage))
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.CONFLICT.name()));

            verify(applicationService).updateUser(eq(TEST_USER_ID), any(UserUpdateRequest.class));
        }
    }

    @Nested
    class UpdatePasswordTests {

        // --- POSITIVE CASE ---

        @Test
        void updatePassword_WithValidRequest_ShouldReturnNoContent() throws Exception {
            mockMvc.perform(patch("/api/v1/users/{id}/password", TEST_USER_ID)
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userPasswordUpdateRequest)))
                    .andExpect(status().isNoContent());

            verify(applicationService).updateUserPassword(eq(TEST_USER_ID), any(UserPasswordUpdateRequest.class));
        }

        // --- NEGATIVE CASES ---

        @ParameterizedTest
        @ArgumentsSource(InvalidPasswordProvider.class)
        void updatePassword_WithInvalidRequest_ShouldReturnBadRequest(String newPassword) throws Exception {
            UserPasswordUpdateRequest request = new UserPasswordUpdateRequest(newPassword);

            mockMvc.perform(patch("/api/v1/users/{id}/password", TEST_USER_ID)
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(applicationService, never()).updateUserPassword(anyLong(), any());
        }

        @Test
        void updatePassword_WhenUserNotFound_ShouldReturnNotFound() throws Exception {
            String expectedMessage = "User not found";

            doThrow(new UserNotFoundException(expectedMessage))
                    .when(applicationService)
                    .updateUserPassword(eq(TEST_USER_ID), eq(userPasswordUpdateRequest));

            mockMvc.perform(patch("/api/v1/users/{id}/password", TEST_USER_ID)
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userPasswordUpdateRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(expectedMessage))
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.NOT_FOUND.name()));

            verify(applicationService).updateUserPassword(eq(TEST_USER_ID), eq(userPasswordUpdateRequest));
        }
    }

    @Nested
    class DeleteUserTests {

        // --- POSITIVE CASE ---

        @Test
        void deleteUser_WithValidId_ShouldReturnNoContent() throws Exception {
            mockMvc.perform(delete("/api/v1/users/{id}", TEST_USER_ID)
                            .with(jwt()))
                    .andExpect(status().isNoContent());

            verify(applicationService).deleteUserById(TEST_USER_ID);
        }

        // --- NEGATIVE CASES ---

        @ParameterizedTest
        @ArgumentsSource(InvalidIdProvider.class)
        void deleteUser_WithInvalidId_ShouldReturnBadRequest(Long id) throws Exception {
            mockMvc.perform(delete("/api/v1/users/{id}", id)
                            .with(jwt()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.BAD_REQUEST.name()));

            verify(applicationService, never()).deleteUserById(anyLong());
        }

        @Test
        void deleteUser_WhenUserNotFound_ShouldReturnNotFound() throws Exception {
            String expectedMessage = "User not found";

            doThrow(new UserNotFoundException(expectedMessage))
                    .when(applicationService)
                    .deleteUserById(TEST_USER_ID);

            mockMvc.perform(delete("/api/v1/users/{id}", TEST_USER_ID)
                            .with(jwt()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(expectedMessage))
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.NOT_FOUND.name()));

            verify(applicationService).deleteUserById(TEST_USER_ID);
        }
    }
}