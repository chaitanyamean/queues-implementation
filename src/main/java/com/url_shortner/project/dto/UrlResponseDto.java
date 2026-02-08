package com.url_shortner.project.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder

public class UrlResponseDto implements Serializable {
    private String shortCode;
    private String originalUrl;
    private String shortUrl;
    private String customCode;
    private String status;
    private String message;
}
