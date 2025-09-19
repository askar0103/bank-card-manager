package com.example.bankcards.entity;

import com.example.bankcards.converter.CardNumberConverter;
import com.example.bankcards.enums.CardStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cards")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Convert(converter = CardNumberConverter.class)
    @Column(name = "encrypted_card_number", nullable = false)
    private String cardNumber;

    @Column(nullable = false, unique = true)
    @EqualsAndHashCode.Include
    private String cardNumberHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardStatus cardStatus;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;
}
