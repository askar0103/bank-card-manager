package com.example.bankcards.service.application;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.domain.CardDomainService;
import com.example.bankcards.service.domain.TransferDomainService;
import com.example.bankcards.service.domain.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferApplicationService {

    private final UserDomainService userDomainService;

    private final CardDomainService cardDomainService;

    private final TransferDomainService transferDomainService;

    @Transactional
    public void transfer(String username, TransferRequest request) {
        User user = userDomainService.getByUsername(username);
        Card fromCard = cardDomainService.getCardByIdAndByOwnerId(
                request.fromCardId(), user.getId()
        );
        Card toCard = cardDomainService.getCardByIdAndByOwnerId(
                request.toCardId(), user.getId()
        );
        transferDomainService.transfer(fromCard, toCard, request.amount());
    }
}
