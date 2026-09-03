package com.harsh.product.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.harsh.product.inventory.dto.request.LoginRequest;
import com.harsh.product.inventory.dto.request.RefreshTokenRequest;
import com.harsh.product.inventory.dto.request.RegisterRequest;
import com.harsh.product.inventory.dto.response.LoginResponse;
import com.harsh.product.inventory.dto.response.RefreshTokenResponse;
import com.harsh.product.inventory.dto.response.RegisterResponse;
import com.harsh.product.inventory.exception.GlobalExceptionHandler;
import com.harsh.product.inventory.exception.InvalidRefreshTokenException;
import com.harsh.product.inventory.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /auth/register - Success returns 201 Created")
    void registerUser_Success() throws Exception {
        RegisterRequest request = new RegisterRequest("Test User", "test@test.com", "Password123!");
        RegisterResponse response = RegisterResponse.builder().success(true).message("User Registered successfully.").build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User Registered successfully."));
    }

    @Test
    @DisplayName("POST /auth/login - Success returns 200 OK with tokens")
    void loginUser_Success() throws Exception {
        String loginJson = "{\"email\":\"test@test.com\",\"password\":\"password123\"}";
        LoginResponse response = LoginResponse.builder()
                .status(true)
                .message("Logged in successfully")
                .accessToken("access-token-xyz")
                .refreshToken("refresh-token-abc")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.accessToken").value("access-token-xyz"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-abc"));
    }

    @Test
    @DisplayName("POST /auth/refresh - Success returns 200 OK with new tokens")
    void refreshToken_Success() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
        RefreshTokenResponse response = RefreshTokenResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .build();

        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"));
    }

    @Test
    @DisplayName("POST /auth/refresh - Invalid refresh token returns 401 Unauthorized")
    void refreshToken_InvalidToken_Returns401() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-refresh-token");

        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new InvalidRefreshTokenException("Invalid refresh token"));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid refresh token"));
    }
}
