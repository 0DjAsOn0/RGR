package com.rgr.messanger.web.dto.auth;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class JwtRequest {

    @NotNull(message = "Поле никнейм не может быть пустым")
    private String username;
    @NotNull(message = "Поле пароль не может быть пустым")
    private String password;

}
