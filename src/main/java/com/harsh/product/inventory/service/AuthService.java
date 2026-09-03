package com.harsh.product.inventory.service;

import com.harsh.product.inventory.dto.request.LoginRequest;
import com.harsh.product.inventory.dto.request.RefreshTokenRequest;
import com.harsh.product.inventory.dto.request.RegisterRequest;
import com.harsh.product.inventory.dto.response.LoginResponse;
import com.harsh.product.inventory.dto.response.RefreshTokenResponse;
import com.harsh.product.inventory.dto.response.RegisterResponse;
import com.harsh.product.inventory.entity.RefreshToken;
import com.harsh.product.inventory.entity.User;
import com.harsh.product.inventory.enums.Role;
import com.harsh.product.inventory.exception.InvalidRefreshTokenException;
import com.harsh.product.inventory.exception.TokenAlreadyExpiredException;
import com.harsh.product.inventory.exception.TokenAlreadyUsedException;
import com.harsh.product.inventory.exception.UserAlreadyExistsException;
import com.harsh.product.inventory.repository.RefreshTokenRepository;
import com.harsh.product.inventory.repository.UserRepository;
import com.harsh.product.inventory.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@AllArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    public RegisterResponse register(RegisterRequest registerRequest) {

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new UserAlreadyExistsException("User already exists");
        }

        User user = User.builder()
                .fullname(registerRequest.getFullName())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(Role.USER)
                .build();

        userRepository.save(user);

        return RegisterResponse.builder()
                .success(true)
                .message("User Registered successfully.")
                .build();
    }

    public LoginResponse login(LoginRequest loginRequest) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                loginRequest.getEmail(),
                loginRequest.getPassword());

        Authentication authenticated = authenticationManager.authenticate(authentication);

        CustomUserDetails user = (CustomUserDetails) authenticated.getPrincipal();

        String token = jwtService.generateJWTToken(
                user.getUsername(),
                user.getUser().getRole().name());

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUser());

        return LoginResponse.builder()
                .status(true)
                .message("Logged in successfully")
                .accessToken(token)
                .refreshToken(refreshToken.getToken())
                .fullName(user.getUser().getFullname())
                .email(user.getUser().getEmail())
                .build();
    }

    @Transactional
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {

        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(request.getRefreshToken())
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new TokenAlreadyUsedException("Refresh token has already been used or revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TokenAlreadyExpiredException(
                    "Refresh token has expired. Please log in again"
            );
        }

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        User user = refreshToken.getUser();

        String newAccessToken = jwtService.generateJWTToken(
                user.getEmail(),
                user.getRole().name()
        );

        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .build();
    }
}
