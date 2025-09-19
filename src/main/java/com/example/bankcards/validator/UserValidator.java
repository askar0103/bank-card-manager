package com.example.bankcards.validator;

import com.example.bankcards.entity.User;
import com.example.bankcards.exception.badrequest.UserOperationNotAllowedException;
import com.example.bankcards.exception.conflict.UserAlreadyExistsException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserValidator {

    private final UserRepository userRepository;
    private final CardRepository cardRepository;

    public void validateForCreate(User user) {
        validateUsernameNotExists(user.getUsername());
    }

    public void validateForUpdateUsername(User user, String newUsername) {
        if (user.getUsername().equals(newUsername)) {
            throw new UserOperationNotAllowedException(
                    "New username cannot be the same as the current username"
            );
        }
        validateUsernameNotExists(newUsername);
    }

    public void validateForDelete(User user) {
        if (cardRepository.existsByOwner(user)) {
            throw new UserOperationNotAllowedException(
                    "User cannot be deleted because they have linked cards"
            );
        }
    }

    private void validateUsernameNotExists(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException(
                    "User '%s' already exists".formatted(username)
            );
        }
    }
}
