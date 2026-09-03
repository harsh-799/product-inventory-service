package com.harsh.product.inventory.service;

import com.harsh.product.inventory.dto.request.LoginRequest;
import com.harsh.product.inventory.dto.request.RegisterRequest;
import com.harsh.product.inventory.dto.response.LoginResponse;
import com.harsh.product.inventory.dto.response.RegisterResponse;
import com.harsh.product.inventory.entity.User;
import com.harsh.product.inventory.enums.Role;
import com.harsh.product.inventory.exception.UserAlreadyExistsException;
import com.harsh.product.inventory.repository.UserRepository;
import com.harsh.product.inventory.security.CustomUserDetails;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

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

        return LoginResponse.builder()
                .status(true)
                .message("Logged in successfully")
                .token(token)
                .fullName(user.getUser().getFullname())
                .email(user.getUser().getEmail())
                .build();
    }


}
