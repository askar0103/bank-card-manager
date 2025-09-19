package com.example.bankcards.validator;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.Role;
import com.example.bankcards.exception.badrequest.UserOperationNotAllowedException;
import com.example.bankcards.exception.conflict.UserAlreadyExistsException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.example.bankcards.util.TestData.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserValidatorTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private UserValidator userValidator;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(TEST_USER_ID)
                .username(TEST_USERNAME)
                .passwordHash(TEST_PASSWORD_HASH)
                .role(Role.USER)
                .build();
    }

    @Nested
    class ValidateForCreateTests {

        @Test
        void validateForCreate_WithNewUsername_ShouldNotThrowException() {
            when(userRepository.existsByUsername(user.getUsername())).thenReturn(false);

            userValidator.validateForCreate(user);
            verify(userRepository).existsByUsername(user.getUsername());
        }

        @Test
        void validateForCreate_WithExistingUsername_ShouldThrowUserAlreadyExistsException() {
            when(userRepository.existsByUsername(user.getUsername())).thenReturn(true);

            assertThatThrownBy(() ->
                    userValidator.validateForCreate(user)
            ).isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessage("User '%s' already exists".formatted(user.getUsername()));

            verify(userRepository).existsByUsername(user.getUsername());
        }
    }

    @Nested
    class ValidateForUpdateUsernameTests {

        @Test
        void validateForUpdateUsername_WithNewUsername_ShouldNotThrowException() {
            when(userRepository.existsByUsername(NEW_USERNAME)).thenReturn(false);

            userValidator.validateForUpdateUsername(user, NEW_USERNAME);
            verify(userRepository).existsByUsername(NEW_USERNAME);
        }

        @Test
        void validateForUpdateUsername_WithExistingUsername_ShouldThrowUserAlreadyExistsException() {
            when(userRepository.existsByUsername(NEW_USERNAME)).thenReturn(true);

            assertThatThrownBy(() ->
                    userValidator.validateForUpdateUsername(user, NEW_USERNAME)
            ).isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessage("User '%s' already exists".formatted(NEW_USERNAME));

            verify(userRepository).existsByUsername(NEW_USERNAME);
        }

        @Test
        void validateForUpdateUsername_WithSameUsername_ShouldThrowUserAlreadyExistsException() {
            assertThatThrownBy(() ->
                    userValidator.validateForUpdateUsername(user, user.getUsername())
            ).isInstanceOf(UserOperationNotAllowedException.class)
                    .hasMessage("New username cannot be the same as the current username");

            verify(userRepository, never()).existsByUsername(TEST_USERNAME);
        }
    }

    @Nested
    class ValidateForDeleteTests {

        @Test
        void validateForDelete_WithUserWithoutCards_ShouldNotThrowException() {
            when(cardRepository.existsByOwner(user)).thenReturn(false);

            userValidator.validateForDelete(user);
            verify(cardRepository).existsByOwner(user);
        }

        @Test
        void validateForDelete_WithUserWithCards_ShouldThrowUserOperationNotAllowedException() {
            user.getCards().add(new Card());
            User userWithCards = user;
            when(cardRepository.existsByOwner(userWithCards)).thenReturn(true);

            assertThatThrownBy(() ->
                    userValidator.validateForDelete(userWithCards)
            ).isInstanceOf(UserOperationNotAllowedException.class)
                    .hasMessage("User cannot be deleted because they have linked cards");

            verify(cardRepository).existsByOwner(userWithCards);
        }
    }

    @Nested
    class ValidateUsernameNotExistsTests {

        @Test
        void validateUsernameNotExists_WithNewUsername_ShouldNotThrowException() {
            when(userRepository.existsByUsername(NEW_USERNAME)).thenReturn(false);

            userValidator.validateForCreate(User.builder().username(NEW_USERNAME).build());
            verify(userRepository).existsByUsername(NEW_USERNAME);
        }

        @Test
        void validateUsernameNotExists_WithExistingUsername_ShouldThrowUserAlreadyExistsException() {
            when(userRepository.existsByUsername("existingUser")).thenReturn(true);

            assertThatThrownBy(() ->
                    userValidator.validateForCreate(User.builder().username("existingUser").build())
            ).isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessage("User 'existingUser' already exists");

            verify(userRepository).existsByUsername("existingUser");
        }
    }
}