package com.url_shortner.project.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UrlRequestDto {
    @NotBlank(message = "URL cannot be empty")
    @Pattern(regexp = "^(http|https)://.*$", message = "URL must start with http:// or https://")
    private String url;

    private LocalDateTime expiryDate;

    private String customCode;
    private String password;
}
