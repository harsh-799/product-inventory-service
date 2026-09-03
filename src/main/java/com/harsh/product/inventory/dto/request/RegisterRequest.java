package com.harsh.product.inventory.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "name is required")
    private String fullName;

    @NotBlank(message = "email is required")
    @Email(message = "Please provide a valid email")
    private String email;

    @NotBlank(message = "password is required")
    @Size(
            min = 8,
            max = 15,
            message = "Password must be between 8 and 15 characters"
    )
    private String password;
}
