package com.rgr.messanger.service;

import org.springframework.scheduling.annotation.Async;

public interface EmailService {
    void sendVerificationEmail(String toEmail, String username, String token);

    @Async
    void sendUnreadNotification(String toEmail, String username,
                                int unreadCount, String chatName);

    void sendPasswordResetCode(String to, String username, String code);
}