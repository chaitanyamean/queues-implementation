package com.url_shortner.project.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserRequestDto {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String name;

    // Accepts "role": "ADMIN"
    private String role;

    // Accepts "roles": ["ADMIN", "USER"]
    private java.util.Set<String> roles;
}
