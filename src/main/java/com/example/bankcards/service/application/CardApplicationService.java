package com.example.bankcards.service.application;

import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.dto.response.UserResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.Role;
import com.example.bankcards.exception.forbidden.CardAccessDeniedException;
import com.example.bankcards.exception.badrequest.CardOperationNotAllowedException;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.mapper.UserMapper;
import com.example.bankcards.service.domain.CardDomainService;
import com.example.bankcards.service.domain.UserDomainService;
import com.example.bankcards.util.CardNumberHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CardApplicationService {

    private final UserDomainService userDomainService;
    private final UserMapper userMapper;

    private final CardDomainService cardDomainService;
    private final CardMapper cardMapper;

    private final CardNumberHasher cardNumberHasher;

    @Transactional
    public CardResponse createCard(CardCreateRequest cardCreateRequest) {
        User owner = userDomainService.getUserById(cardCreateRequest.ownerId());
        String cardNumberHash = cardNumberHasher.hash(cardCreateRequest.cardNumber());

        Card cardToCreate = cardMapper.toCard(cardCreateRequest, cardNumberHash, owner);

        Card newCard = cardDomainService.createCard(cardToCreate);

        return cardMapper.toCardResponse(newCard);
    }

    @Transactional
    public CardResponse getCardByIdForUser(Long cardId, String username) {
        Card card = cardDomainService.getCardById(cardId);

        if (card.getOwner().getUsername().equals(username)) {
            return cardMapper.toCardResponse(card);
        }

        User user = userDomainService.getByUsername(username);
        if (user.getRole() == Role.ADMIN) {
            log.info("Admin '{}' accessed card ID {}", username, cardId);
            return cardMapper.toCardResponse(card);
        }

        log.warn("User '{}' denied access to card ID {}", username, cardId);
        throw new CardAccessDeniedException(
                "User '%s' does not have access to card with id %d".formatted(username, cardId)
        );
    }

    @Transactional
    public UserResponse getOwnerByCardId(Long cardId) {
        Card card = cardDomainService.getCardById(cardId);
        User owner = card.getOwner();
        return userMapper.toUserResponse(owner);
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> getCardsForUser(String username, Pageable pageable) {
        Page<Card> cards;

        User user = userDomainService.getByUsername(username);
        if (user.getRole() == Role.ADMIN) {
            cards = cardDomainService.getCards(pageable);
            log.info("Admin '{}' retrieved {} cards (page={}, size={})",
                    username, cards.getNumberOfElements(), pageable.getPageNumber(), pageable.getPageSize());
        } else {
            cards = cardDomainService.getCardsByOwner(user, pageable);
            log.info("User '{}' retrieved {} own cards (page={}, size={})",
                    username, cards.getNumberOfElements(), pageable.getPageNumber(), pageable.getPageSize());
        }

        return cards.map(cardMapper::toCardResponse);
    }

    @Transactional
    public CardResponse blockCardById(Long cardId, String username) {
        Card card = cardDomainService.getCardById(cardId);

        if (card.getOwner().getUsername().equals(username)) {
            card = cardDomainService.blockCard(card);
            return cardMapper.toCardResponse(card);
        }

        User user = userDomainService.getByUsername(username);
        if (user.getRole() == Role.ADMIN) {
            card = cardDomainService.blockCard(card);
            return cardMapper.toCardResponse(card);
        }

        log.warn("User '{}' denied access to block card ID {}", username, cardId);
        throw new CardOperationNotAllowedException("Access denied to block this card");
    }

    @Transactional
    public CardResponse activateById(Long cardId) {
        Card card = cardDomainService.getCardById(cardId);
        card = cardDomainService.activateCard(card);
        return cardMapper.toCardResponse(card);
    }

    @Transactional
    public void deleteCardById(Long cardId) {
        Card card = cardDomainService.getCardById(cardId);
        cardDomainService.deleteCard(card);
    }
}
