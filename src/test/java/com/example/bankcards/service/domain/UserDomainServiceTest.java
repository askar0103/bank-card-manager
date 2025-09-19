package com.example.bankcards.service.domain;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.enums.Role;
import com.example.bankcards.exception.badrequest.UserOperationNotAllowedException;
import com.example.bankcards.exception.conflict.UserAlreadyExistsException;
import com.example.bankcards.exception.notfound.UserNotFoundException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.validator.UserValidator;
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

import java.util.List;
import java.util.Optional;

import static com.example.bankcards.util.TestData.*;
import static com.example.bankcards.util.TestData.POSITIVE_BALANCE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDomainServiceTest {

    @Mock
    private UserValidator userValidator;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDomainService userDomainService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(TEST_USER_ID)
                .username(TEST_USERNAME)
                .passwordHash(TEST_PASSWORD_HASH)
                .role(Role.USER)
                .build();

        Card card = Card.builder()
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
    class createUserTests {

        @Test
        void createUser_WithValidUser_ShouldReturnSavedUser() {
            doNothing().when(userValidator).validateForCreate(user);
            when(userRepository.save(user)).thenReturn(user);

            User result = userDomainService.createUser(user);

            assertThat(result).isEqualTo(user);
            verify(userValidator).validateForCreate(user);
            verify(userRepository).save(user);
        }

        @Test
        void createUser_WithExistingUsername_ShouldThrowUserAlreadyExistsException() {
            String expectedMessage = "User '%s' already exists".formatted(user.getUsername());
            doThrow(new UserAlreadyExistsException(expectedMessage))
                    .when(userValidator).validateForCreate(user);

            assertThatThrownBy(() ->
                    userDomainService.createUser(user)
            ).isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining(expectedMessage);
        }
    }

    @Nested
    class getUserByIdTests {

        @Test
        void getUserById_WithExistingId_ShouldReturnUser() {
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

            User result = userDomainService.getUserById(user.getId());

            assertThat(result).isEqualTo(user);
            verify(userRepository).findById(user.getId());
        }

        @Test
        void getUserById_WithNonExistingId_ShouldThrowUserNotFoundException() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    userDomainService.getUserById(999L)
            ).isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("User with id 999 not found");
        }
    }

    @Nested
    class getByUsernameTests {

        @Test
        void getByUsername_WithExistingUsername_ShouldReturnUser() {
            when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

            User result = userDomainService.getByUsername(user.getUsername());

            assertThat(result).isEqualTo(user);
            verify(userRepository).findByUsername(user.getUsername());
        }

        @Test
        void getByUsername_WithNonExistingUsername_ShouldThrowUserNotFoundException() {
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    userDomainService.getByUsername("nonexistent")
            ).isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("User with username \"nonexistent\" not found");
        }
    }

    @Nested
    class getUsersTests {

        @Test
        void getUsers_WithPageable_ShouldReturnPageOfUsers() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> userPage = new PageImpl<>(List.of(user));

            when(userRepository.findAll(pageable)).thenReturn(userPage);

            Page<User> result = userDomainService.getUsers(pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(userRepository).findAll(pageable);
        }
    }

    @Nested
    class updateUsernameTests {

        @Test
        void updateUsername_WithValidNewUsername_ShouldUpdateUsername() {
            doNothing().when(userValidator).validateForUpdateUsername(user, NEW_USERNAME);

            User result = userDomainService.updateUsername(user, NEW_USERNAME);

            assertThat(result.getUsername()).isEqualTo(NEW_USERNAME);
            verify(userValidator).validateForUpdateUsername(user, NEW_USERNAME);
        }

        @Test
        void updateUsername_WithExistingUsername_ShouldThrowUserAlreadyExistsException() {
            String expectedMessage = "User '%s' already exists".formatted(NEW_USERNAME);
            doThrow(new UserAlreadyExistsException(expectedMessage))
                    .when(userValidator).validateForUpdateUsername(user, NEW_USERNAME);

            assertThatThrownBy(() ->
                    userDomainService.updateUsername(user, NEW_USERNAME)
            ).isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining(expectedMessage);
        }
    }

    @Nested
    class saveUserTests {

        @Test
        void saveUser_WithValidUser_ShouldSaveUser() {
            when(userRepository.save(user)).thenReturn(user);

            userDomainService.saveUser(user);

            verify(userRepository).save(user);
        }
    }

    @Nested
    class deleteUserByIdTests {

        @Test
        void deleteUserById_WithValidUser_ShouldDeleteUser() {
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            doNothing().when(userValidator).validateForDelete(user);
            doNothing().when(userRepository).delete(user);

            userDomainService.deleteUserById(user.getId());

            verify(userRepository).findById(user.getId());
            verify(userValidator).validateForDelete(user);
            verify(userRepository).delete(user);
        }

        @Test
        void deleteUserById_WithNonExistingId_ShouldThrowUserNotFoundException() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    userDomainService.deleteUserById(999L)
            ).isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("User with id 999 not found");
        }

        @Test
        void deleteUserById_WithUserHavingLinkedCards_ShouldThrowUserOperationNotAllowedException() {
            String expectedMessage ="User cannot be deleted because they have linked cards";
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            doThrow(new UserOperationNotAllowedException(
                    expectedMessage
            )).when(userValidator).validateForDelete(user);

            assertThatThrownBy(() ->
                    userDomainService.deleteUserById(1L)
            ).isInstanceOf(UserOperationNotAllowedException.class)
                    .hasMessageContaining(expectedMessage);
        }
    }
}