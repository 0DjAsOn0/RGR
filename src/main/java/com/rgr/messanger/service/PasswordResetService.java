package com.rgr.messanger.service;

public interface PasswordResetService {

    void sendResetCode(String email);

    boolean verifyCode(String email, String code);

    void resetPassword(String email, String newPassword);
}