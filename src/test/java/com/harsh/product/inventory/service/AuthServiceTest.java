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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .fullname("Test User")
                .password("encodedPassword")
                .role(Role.USER)
                .build();
    }

    private LoginRequest createLoginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "password", password);
        return request;
    }

    @Test
    @DisplayName("Should successfully authenticate user and return both access and refresh tokens")
    void login_Success() {
        LoginRequest request = createLoginRequest("test@example.com", "password123");
        CustomUserDetails userDetails = new CustomUserDetails(testUser);
        Authentication authentication = mock(Authentication.class);

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtService.generateJWTToken("test@example.com", "USER")).thenReturn("mock-access-token");

        RefreshToken mockRefreshToken = RefreshToken.builder()
                .token("mock-refresh-token")
                .user(testUser)
                .build();
        when(refreshTokenService.createRefreshToken(testUser)).thenReturn(mockRefreshToken);

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertTrue(response.isStatus());
        assertEquals("mock-access-token", response.getAccessToken());
        assertEquals("mock-refresh-token", response.getRefreshToken());
        assertEquals("Test User", response.getFullName());
        assertEquals("test@example.com", response.getEmail());

        verify(jwtService).generateJWTToken("test@example.com", "USER");
        verify(refreshTokenService).createRefreshToken(testUser);
    }

    @Test
    @DisplayName("Should propagate BadCredentialsException when authentication fails")
    void login_InvalidCredentials() {
        LoginRequest request = createLoginRequest("test@example.com", "wrong-password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verifyNoInteractions(jwtService);
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    @DisplayName("Should successfully rotate refresh token and return new access and refresh tokens")
    void refreshToken_Success() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
        RefreshToken oldToken = RefreshToken.builder()
                .id(1L)
                .token("valid-refresh-token")
                .user(testUser)
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build();

        when(refreshTokenRepository.findByToken("valid-refresh-token"))
                .thenReturn(Optional.of(oldToken));
        when(jwtService.generateJWTToken("test@example.com", "USER"))
                .thenReturn("new-access-token");

        RefreshToken newToken = RefreshToken.builder()
                .token("new-refresh-token")
                .user(testUser)
                .build();
        when(refreshTokenService.createRefreshToken(testUser)).thenReturn(newToken);

        RefreshTokenResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());

        assertTrue(oldToken.isRevoked(), "Old token should be marked revoked");
        verify(refreshTokenRepository).save(oldToken);
        verify(refreshTokenService).createRefreshToken(testUser);
        verify(jwtService).generateJWTToken("test@example.com", "USER");
    }

    @Test
    @DisplayName("Should throw InvalidRefreshTokenException when token does not exist in DB")
    void refreshToken_InvalidToken() {
        RefreshTokenRequest request = new RefreshTokenRequest("non-existent-token");

        when(refreshTokenRepository.findByToken("non-existent-token"))
                .thenReturn(Optional.empty());

        InvalidRefreshTokenException ex = assertThrows(InvalidRefreshTokenException.class,
                () -> authService.refreshToken(request));

        assertEquals("Invalid refresh token", ex.getMessage());
        verify(refreshTokenRepository, never()).save(any());
        verifyNoInteractions(jwtService);
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    @DisplayName("Should throw TokenAlreadyUsedException when refresh token is already revoked")
    void refreshToken_AlreadyRevoked() {
        RefreshTokenRequest request = new RefreshTokenRequest("revoked-token");
        RefreshToken revokedToken = RefreshToken.builder()
                .token("revoked-token")
                .user(testUser)
                .revoked(true)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build();

        when(refreshTokenRepository.findByToken("revoked-token"))
                .thenReturn(Optional.of(revokedToken));

        TokenAlreadyUsedException ex = assertThrows(TokenAlreadyUsedException.class,
                () -> authService.refreshToken(request));

        assertEquals("Refresh token has already been used or revoked", ex.getMessage());
        verify(refreshTokenRepository, never()).save(any());
        verifyNoInteractions(jwtService);
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    @DisplayName("Should throw TokenAlreadyExpiredException when refresh token is expired")
    void refreshToken_Expired() {
        RefreshTokenRequest request = new RefreshTokenRequest("expired-token");
        RefreshToken expiredToken = RefreshToken.builder()
                .token("expired-token")
                .user(testUser)
                .revoked(false)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        when(refreshTokenRepository.findByToken("expired-token"))
                .thenReturn(Optional.of(expiredToken));

        TokenAlreadyExpiredException ex = assertThrows(TokenAlreadyExpiredException.class,
                () -> authService.refreshToken(request));

        assertEquals("Refresh token has expired. Please log in again", ex.getMessage());
        verify(refreshTokenRepository, never()).save(any());
        verifyNoInteractions(jwtService);
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    @DisplayName("Should enforce token rotation: old token R1 is revoked after refresh, and reusing R1 throws TokenAlreadyUsedException")
    void refreshToken_RotationSequence_ReusedTokenFails() {
        RefreshToken r1 = RefreshToken.builder()
                .token("R1")
                .user(testUser)
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(refreshTokenRepository.findByToken("R1")).thenReturn(Optional.of(r1));
        when(jwtService.generateJWTToken(anyString(), anyString())).thenReturn("A2");
        when(refreshTokenService.createRefreshToken(testUser)).thenReturn(
                RefreshToken.builder().token("R2").user(testUser).build());

        // Step 1: Client refreshes with R1
        RefreshTokenResponse response = authService.refreshToken(new RefreshTokenRequest("R1"));
        assertEquals("A2", response.getAccessToken());
        assertEquals("R2", response.getRefreshToken());
        assertTrue(r1.isRevoked(), "R1 must be revoked after first refresh");

        // Step 2: Client or attacker tries to reuse R1
        TokenAlreadyUsedException ex = assertThrows(TokenAlreadyUsedException.class,
                () -> authService.refreshToken(new RefreshTokenRequest("R1")));
        assertEquals("Refresh token has already been used or revoked", ex.getMessage());
    }

    @Test
    @DisplayName("Should successfully register a new user with default role USER")
    void register_Success() {
        RegisterRequest request = new RegisterRequest("New User", "new@example.com", "Password123!");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encodedPassword");

        RegisterResponse response = authService.register(request);

        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals("User Registered successfully.", response.getMessage());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when registering existing email")
    void register_UserAlreadyExists() {
        RegisterRequest request = new RegisterRequest("Existing User", "existing@example.com", "Password123!");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }
}
