package com.url_shortner.project.dto;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BatchUrlResponseDto {
    private String originalUrl;
    private String shortUrl;
    private String error; // Null if success, error message if failed
    private String status; // "SUCCESS" or "FAILURE"
}
