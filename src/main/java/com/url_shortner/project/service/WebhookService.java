package com.url_shortner.project.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

@Service
public class WebhookService {

    @Value("${webhook.analytics.url}")
    private String webhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendAnalytics(Long imageId, String eventType) {
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("placeholder")) {
            System.out.println("[Webhook] URL not configured or placeholder. Skipping.");
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("imageId", imageId);
            payload.put("event", eventType);
            payload.put("timestamp", System.currentTimeMillis());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            restTemplate.postForLocation(webhookUrl, request);
            System.out.println("[Webhook] Sent analytics for Image ID: " + imageId);
        } catch (Exception e) {
            System.err.println("[Webhook] Failed to send analytics: " + e.getMessage());
        }
    }
}
