package com.example.bankcards.service.domain;

import com.example.bankcards.entity.User;
import com.example.bankcards.exception.notfound.UserNotFoundException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDomainService {

    private final UserValidator userValidator;
    private final UserRepository userRepository;

    public User createUser(User user) {
        userValidator.validateForCreate(user);
        return userRepository.save(user);
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(
                        "User with id %d not found".formatted(userId)
                ));
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(
                        "User with username \"%s\" not found".formatted(username)
                ));
    }

    public Page<User> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public User updateUsername(User user, String newUsername) {
        userValidator.validateForUpdateUsername(user, newUsername);
        user.setUsername(newUsername);
        return user;
    }

    public void saveUser(User user) {
        userRepository.save(user);
    }

    public void deleteUserById(Long userId) {
        User user = getUserById(userId);
        userValidator.validateForDelete(user);
        userRepository.delete(user);
    }
}
