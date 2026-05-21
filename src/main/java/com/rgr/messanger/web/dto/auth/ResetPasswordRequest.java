package com.rgr.messanger.web.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, max = 100, message = "Пароль от 6 до 100 символов")
    private String newPassword;

    @NotBlank
    private String newPasswordConfirmation;
}