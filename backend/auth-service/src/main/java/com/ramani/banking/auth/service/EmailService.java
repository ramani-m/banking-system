package com.ramani.banking.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Async
    public void sendPasswordResetEmail(String to, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Password Reset Request — Ramani Banking");
            message.setText("""
                    Hello,
                    
                    You requested a password reset for your Ramani Banking account.
                    
                    Click the link below to reset your password (valid for 1 hour):
                    %s/reset-password?token=%s
                    
                    If you did not request this, please ignore this email.
                    
                    — Ramani Banking Team
                    """.formatted(frontendUrl, token));
            mailSender.send(message);
            log.info("Password reset email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendWelcomeEmail(String to, String firstName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Welcome to Ramani Banking!");
            message.setText("""
                    Hello %s,
                    
                    Welcome to Ramani Banking! Your account has been created successfully.
                    
                    You can now log in at: %s
                    
                    — Ramani Banking Team
                    """.formatted(firstName, frontendUrl));
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", to, e.getMessage());
        }
    }
}
