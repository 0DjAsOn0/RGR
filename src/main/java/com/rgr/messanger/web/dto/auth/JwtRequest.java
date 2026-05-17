package com.rgr.messanger.web.dto.auth;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class JwtRequest {

    @NotNull(message = "Поле почта не может быть пустым")
    private String email;
    @NotNull(message = "Поле пароль не может быть пустым")
    private String password;
    private boolean rememberMe;

}
