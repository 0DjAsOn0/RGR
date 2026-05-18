package com.rgr.messanger.web.dto.auth;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String email;
    private String newPassword;
    private String newPasswordConfirmation;
}