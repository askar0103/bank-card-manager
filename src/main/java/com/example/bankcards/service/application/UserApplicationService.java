package com.example.bankcards.service.application;

import com.example.bankcards.dto.request.UserCreateRequest;
import com.example.bankcards.dto.request.UserPasswordUpdateRequest;
import com.example.bankcards.dto.request.UserUpdateRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.dto.response.UserAuthResponse;
import com.example.bankcards.dto.response.UserResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.mapper.UserMapper;
import com.example.bankcards.service.domain.CardDomainService;
import com.example.bankcards.service.domain.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserApplicationService {

    private final PasswordEncoder passwordEncoder;

    private final UserDomainService userDomainService;
    private final UserMapper userMapper;

    private final CardDomainService cardDomainService;
    private final CardMapper cardMapper;

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        String passwordHash = passwordEncoder.encode(request.password());
        User user = userMapper.toUser(request, passwordHash);
        user = userDomainService.createUser(user);
        return userMapper.toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        User user = userDomainService.getUserById(userId);
        return userMapper.toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userDomainService.getByUsername(username);
        return userMapper.toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getUsers(Pageable pageable) {
        Page<User> users = userDomainService.getUsers(pageable);
        return users.map(userMapper::toUserResponse);
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> getCardsForUser(String username, Pageable pageable) {
        User user = userDomainService.getByUsername(username);
        Page<Card> cards = cardDomainService.getCardsByOwner(user, pageable);
        return cards.map(cardMapper::toCardResponse);
    }

    @Transactional
    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
        User user = userDomainService.getUserById(userId);
        user = userDomainService.updateUsername(user, request.newUsername());
        return userMapper.toUserResponse(user);
    }

    @Transactional
    public void updateUserPassword(Long userId, UserPasswordUpdateRequest request) {
        User user = userDomainService.getUserById(userId);
        String newPasswordHash = passwordEncoder.encode(request.newPassword());
        user.setPasswordHash(newPasswordHash);
        userDomainService.saveUser(user);
    }

    @Transactional(readOnly = true)
    public UserAuthResponse getUserAuthByUsername(String username) {
        User user = userDomainService.getByUsername(username);
        return userMapper.toUserAuthResponse(user, true);
    }

    @Transactional
    public void deleteUserById(Long userId) {
        userDomainService.deleteUserById(userId);
    }
}
