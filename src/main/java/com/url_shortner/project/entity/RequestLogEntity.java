package com.url_shortner.project.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "request_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class RequestLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String httpMethod;      // GET, POST, etc.
    private String url;             // /shorten
    private String clientIp;        // 192.168.1.1
    private String userAgent;       // Chrome/Mozilla...

    private LocalDateTime timestamp;
}
