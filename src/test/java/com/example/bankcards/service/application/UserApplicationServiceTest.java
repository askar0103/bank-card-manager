package com.example.bankcards.service.application;

import com.example.bankcards.dto.request.UserCreateRequest;
import com.example.bankcards.dto.request.UserPasswordUpdateRequest;
import com.example.bankcards.dto.request.UserUpdateRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.dto.response.UserAuthResponse;
import com.example.bankcards.dto.response.UserResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.enums.Role;
import com.example.bankcards.exception.badrequest.UserOperationNotAllowedException;
import com.example.bankcards.exception.conflict.UserAlreadyExistsException;
import com.example.bankcards.exception.notfound.UserNotFoundException;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.mapper.UserMapper;
import com.example.bankcards.service.domain.CardDomainService;
import com.example.bankcards.service.domain.UserDomainService;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static com.example.bankcards.util.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserApplicationServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserDomainService userDomainService;

    @Spy
    private UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @Mock
    private CardDomainService cardDomainService;

    @Spy
    private CardMapper cardMapper = Mappers.getMapper(CardMapper.class);

    @InjectMocks
    private UserApplicationService applicationService;

    private User user;
    private Card card;
    private UserCreateRequest userCreateRequest;
    private UserUpdateRequest userUpdateRequest;
    private UserPasswordUpdateRequest userPasswordUpdateRequest;
    private UserResponse userResponse;
    private UserAuthResponse userAuthResponse;
    private CardResponse cardResponse;

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

        userCreateRequest = new UserCreateRequest(
                TEST_USERNAME,
                TEST_USER_PASSWORD,
                Role.USER
        );

        userUpdateRequest = new UserUpdateRequest(
                NEW_USERNAME
        );

        userPasswordUpdateRequest = new UserPasswordUpdateRequest(
                NEW_USER_PASSWORD
        );

        userResponse = new UserResponse(
                TEST_USER_ID,
                TEST_USERNAME,
                Role.USER
        );

        userAuthResponse = new UserAuthResponse(
                TEST_USER_ID,
                TEST_USERNAME,
                TEST_PASSWORD_HASH,
                true,
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

        @Test
        void createUser_WithValidRequest_ShouldReturnUserResponse() {
            when(passwordEncoder.encode(TEST_USER_PASSWORD)).thenReturn(user.getPasswordHash());
            when(userDomainService.createUser(any(User.class))).thenReturn(user);

            UserResponse result = applicationService.createUser(userCreateRequest);

            assertThat(result).isEqualTo(userResponse);
            verify(passwordEncoder).encode(TEST_USER_PASSWORD);
            verify(userDomainService).createUser(any(User.class));
        }

        @Test
        void createUser_WhenUsernameAlreadyExists_ShouldThrowUserAlreadyExistsException() {
            String expectedMessage = "User '%s' already exists".formatted(TEST_USERNAME);
            when(passwordEncoder.encode(TEST_USER_PASSWORD)).thenReturn(user.getPasswordHash());
            when(userDomainService.createUser(any(User.class))).thenThrow(
                    new UserAlreadyExistsException(expectedMessage)
            );

            assertThatThrownBy(() ->
                    applicationService.createUser(userCreateRequest)
            ).isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining(expectedMessage);
        }
    }

    @Nested
    class GetUserByIdTests {

        @Test
        void getUserById_WithValidId_ShouldReturnUserResponse() {
            when(userDomainService.getUserById(user.getId())).thenReturn(user);

            UserResponse result = applicationService.getUserById(user.getId());

            assertThat(result).isEqualTo(userResponse);
            verify(userDomainService).getUserById(user.getId());
        }

        @Test
        void getUserById_WhenUserNotFound_ShouldThrowUserNotFoundException() {
            String expectedMessage = "User not found";
            when(userDomainService.getUserById(user.getId())).thenThrow(
                    new UserNotFoundException(expectedMessage)
            );

            assertThatThrownBy(() ->
                    applicationService.getUserById(user.getId())
            ).isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining(expectedMessage);

            verify(userDomainService).getUserById(user.getId());
        }
    }

    @Nested
    class GetUserByUsernameTests {

        @Test
        void getUserByUsername_WithValidUsername_ShouldReturnUserResponse() {
            when(userDomainService.getByUsername(user.getUsername())).thenReturn(user);

            UserResponse result = applicationService.getUserByUsername(user.getUsername());

            assertThat(result).isEqualTo(userResponse);

            verify(userDomainService).getByUsername(user.getUsername());
        }

        @Test
        void getUserByUsername_WhenUserNotFound_ShouldThrowUserNotFoundException() {
            String expectedMessage = "User not found";
            when(userDomainService.getByUsername(user.getUsername())).thenThrow(
                    new UserNotFoundException(expectedMessage)
            );

            assertThatThrownBy(() ->
                    applicationService.getUserByUsername(user.getUsername())
            ).isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining(expectedMessage);

            verify(userDomainService).getByUsername(user.getUsername());
        }
    }

    @Nested
    class GetUsersTests {

        @Test
        void getUsers_WithPageable_ShouldReturnPageOfUserResponses() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> userPage = new PageImpl<>(List.of(user));

            when(userDomainService.getUsers(any(Pageable.class))).thenReturn(userPage);

            Page<UserResponse> result = applicationService.getUsers(pageable);

            assertThat(result.getContent()).containsExactly(userResponse);

            verify(userDomainService).getUsers(pageable);
        }
    }

    @Nested
    class GetCardsForUserTests {

        @Test
        void getCardsForUser_WithValidUsername_ShouldReturnPageOfCardResponses() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Card> cardPage = new PageImpl<>(List.of(card));

            when(userDomainService.getByUsername(user.getUsername())).thenReturn(user);
            when(cardDomainService.getCardsByOwner(eq(user), any(Pageable.class))).thenReturn(cardPage);// Act

            Page<CardResponse> result = applicationService.getCardsForUser(user.getUsername(), pageable);

            assertThat(result.getContent()).containsExactly(cardResponse);
            verify(userDomainService).getByUsername(user.getUsername());
            verify(cardDomainService).getCardsByOwner(eq(user), any(Pageable.class));
        }
    }

    @Nested
    class UpdateUserTests {

        @Test
        void updateUser_WithNewUsername_ShouldReturnUpdatedUserResponse() {
            UserResponse updatedResponse = new UserResponse(user.getId(), NEW_USERNAME, Role.USER);
            user.setUsername(NEW_USERNAME);
            when(userDomainService.getUserById(user.getId())).thenReturn(user);
            when(userDomainService.updateUsername(user, user.getUsername())).thenReturn(user);

            UserResponse result = applicationService.updateUser(user.getId(), userUpdateRequest);

            assertThat(result).isEqualTo(updatedResponse);
            assertThat(result.username()).isEqualTo(user.getUsername());

            verify(userDomainService).updateUsername(user, user.getUsername());
        }

        @Test
        void updateUser_WhenUsernameAlreadyExists_ShouldThrowUserAlreadyExistsException() {
            String existingUsername = "existingUser";
            UserUpdateRequest updateRequest = new UserUpdateRequest(existingUsername);
            String expectedMessage = "User '%s' already exists".formatted(existingUsername);

            when(userDomainService.getUserById(user.getId())).thenReturn(user);
            when(userDomainService.updateUsername(user, existingUsername)).thenThrow(
                    new UserAlreadyExistsException(expectedMessage)
            );

            assertThatThrownBy(() ->
                    applicationService.updateUser(user.getId(), updateRequest)
            ).isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining(expectedMessage);

            verify(userDomainService).getUserById(user.getId());
            verify(userDomainService).updateUsername(user, existingUsername);
        }

        @Test
        void updateUser_WhenUserNotFound_ShouldThrowUserNotFoundException() {
            String expectedMessage = "User not found";
            when(userDomainService.getUserById(user.getId())).thenThrow(
                    new UserNotFoundException(expectedMessage)
            );

            assertThatThrownBy(() ->
                    applicationService.updateUser(user.getId(), userUpdateRequest)
            ).isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining(expectedMessage);

            verify(userDomainService).getUserById(user.getId());
            verify(userDomainService, never()).updateUsername(any(User.class), anyString());
        }
    }

    @Nested
    class UpdateUserPasswordTests {

        @Test
        void updateUserPassword_WithValidRequest_ShouldUpdatePassword() {
            when(userDomainService.getUserById(user.getId())).thenReturn(user);
            when(passwordEncoder.encode(NEW_USER_PASSWORD)).thenReturn(user.getPasswordHash());

            applicationService.updateUserPassword(TEST_USER_ID, userPasswordUpdateRequest);

            verify(userDomainService).getUserById(user.getId());
            verify(userDomainService).saveUser(user);
            verify(passwordEncoder).encode(NEW_USER_PASSWORD);
        }

        @Test
        void updateUserPassword_WhenUserNotFound_ShouldThrowUserNotFoundException() {
            String expectedMessage = "User not found";
            when(userDomainService.getUserById(user.getId())).thenThrow(
                    new UserNotFoundException(expectedMessage)
            );

            assertThatThrownBy(() ->
                    applicationService.updateUserPassword(user.getId(), userPasswordUpdateRequest)
            ).isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining(expectedMessage);

            verify(userDomainService).getUserById(user.getId());
            verify(userDomainService, never()).saveUser(user);
            verify(passwordEncoder, never()).encode(NEW_USER_PASSWORD);
        }
    }

    @Nested
    class GetUserAuthByUsernameTests {

        @Test
        void getUserAuthByUsername_WithValidUsername_ShouldReturnUserAuthResponse() {
            when(userDomainService.getByUsername(user.getUsername())).thenReturn(user);

            UserAuthResponse result = applicationService.getUserAuthByUsername(user.getUsername());

            assertThat(result).isEqualTo(userAuthResponse);
            verify(userDomainService).getByUsername(user.getUsername());
        }
    }

    @Nested
    class DeleteUserByIdTests {

        @Test
        void deleteUserById_WithValidId_ShouldDeleteUser() {
            doNothing().when(userDomainService).deleteUserById(TEST_USER_ID);

            applicationService.deleteUserById(TEST_USER_ID);

            verify(userDomainService).deleteUserById(TEST_USER_ID);
        }

        @Test
        void deleteUserById_WithLinkedCards_ShouldThrowUserOperationNotAllowedException() {
            String expectedMessage = "User cannot be deleted because they have linked cards";
            doThrow(new UserOperationNotAllowedException(
                    expectedMessage
            )).when(userDomainService).deleteUserById(TEST_USER_ID);

            assertThatThrownBy(() ->
                    applicationService.deleteUserById(TEST_USER_ID)
            ).isInstanceOf(UserOperationNotAllowedException.class)
                    .hasMessageContaining(expectedMessage);
        }
    }
}