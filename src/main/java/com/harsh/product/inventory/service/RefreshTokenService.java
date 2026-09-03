package com.harsh.product.inventory.service;

import com.harsh.product.inventory.entity.RefreshToken;
import com.harsh.product.inventory.entity.User;
import com.harsh.product.inventory.repository.RefreshTokenRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@AllArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    public RefreshToken createRefreshToken(User user) {

        RefreshToken refreshToken = RefreshToken.builder()
                .token(generateRefreshToken())
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }
}
