package com.url_shortner.project.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponseDto {
    private Long id;
    private String email;
    private String name;
    private String apiKey;
    private LocalDateTime createdAt;
    private String token;
}
