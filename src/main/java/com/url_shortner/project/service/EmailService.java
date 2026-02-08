package com.url_shortner.project.service;

public interface EmailService {
    void sendEmail(String to, String subject, String body);
}
