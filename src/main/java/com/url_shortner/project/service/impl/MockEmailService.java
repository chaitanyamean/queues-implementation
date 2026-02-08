package com.url_shortner.project.service.impl;

import com.url_shortner.project.service.EmailService;
import org.springframework.stereotype.Service;

@Service
public class MockEmailService implements EmailService {

    @Override
    public void sendEmail(String to, String subject, String body) {
        System.out.println("\n---------------------------------------------------");
        System.out.println("[MockEmailService] 📧 Sending Email Alert");
        System.out.println("   To: " + to);
        System.out.println("   Subject: " + subject);
        System.out.println("   Body: " + body);
        System.out.println("---------------------------------------------------\n");
    }
}
