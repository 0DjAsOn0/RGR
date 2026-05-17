package com.rgr.messanger.service;

public interface EmailVerificationService {
    void sendVerification(String email, String username);
    void verifyToken(String token);
}
