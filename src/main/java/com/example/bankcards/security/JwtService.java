package com.example.bankcards.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final JwsHeader JWS_HEADER = JwsHeader.with(() -> "HS256").build();

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.expiration-seconds}")
    private int expirationSeconds;

    private final JwtEncoder jwtEncoder;

    public String generateToken(CustomUserDetails userDetails) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expirationSeconds))
                .subject(userDetails.getUsername())
                .claim("role", userDetails.getRole())
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(JWS_HEADER, claims)
        ).getTokenValue();
    }
}
