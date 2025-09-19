package com.example.bankcards.mapper;

import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.util.CardNumberMasker;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface CardMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cardStatus", expression = "java(CardStatus.ACTIVE)")
    @Mapping(target = "balance", source = "dto.initialBalance")
    Card toCard(CardCreateRequest dto,
                String cardNumberHash,
                User owner);

    @Mapping(target = "maskedCardNumber",
            source = "card.cardNumber",
            qualifiedByName = "maskCardNumber")
    CardResponse toCardResponse(Card card);

    @Named("maskCardNumber")
    default String maskCardNumber(String cardNumber) {
        return CardNumberMasker.mask(cardNumber);
    }
}
