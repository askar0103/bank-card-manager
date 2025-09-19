package com.example.bankcards.service.domain;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.exception.notfound.CardNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.validator.CardValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor
public class CardDomainService {

    private final CardValidator cardValidator;
    private final CardRepository cardRepository;

    public Card createCard(Card card) {
        cardValidator.validateForCreate(card);
        return cardRepository.save(card);
    }

    public Card getCardById(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(
                        "Card with id %d not found".formatted(cardId)
                ));

        return updateStatusIfExpired(card);
    }

    public Card getCardByIdAndByOwnerId(Long cardId, Long userId) {
        Card card = cardRepository.findByIdAndOwner_Id(cardId, userId)
                .orElseThrow(() -> new CardNotFoundException(
                        "Card not found or does not belong to the user"
                ));

        return updateStatusIfExpired(card);
    }

    public Page<Card> getCards(Pageable pageable) {
        return cardRepository.findAll(pageable);
    }

    public Page<Card> getCardsByOwner(User owner, Pageable pageable) {
        return cardRepository.findAllByOwner(owner, pageable);
    }

    public Card blockCard(Card card) {
        cardValidator.validateForBlock(card);
        card.setCardStatus(CardStatus.BLOCKED);
        return card;
    }

    public Card activateCard(Card card) {
        cardValidator.validateForActivate(card);
        card.setCardStatus(CardStatus.ACTIVE);
        return card;
    }

    public void deleteCard(Card card) {
        cardValidator.validateForDelete(card);
        cardRepository.delete(card);
    }

    private Card updateStatusIfExpired(Card card) {
        if (card.getExpiryDate().isBefore(LocalDate.now())
                && card.getCardStatus() != CardStatus.EXPIRED) {
            card.setCardStatus(CardStatus.EXPIRED);
            card = cardRepository.save(card);
            log.info("Card ID {} marked as EXPIRED", card.getId());
        }
        return card;
    }
}
