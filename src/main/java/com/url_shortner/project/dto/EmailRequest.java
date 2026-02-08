package com.url_shortner.project.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EmailRequest {
    private String email;
    private String subject;
    private String body;
    private LocalDateTime scheduledTime; // "2026-08-15T10:30:00"
}
