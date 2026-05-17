package com.rgr.messanger.web.dto.user;

import com.rgr.messanger.web.dto.validation.OnCreate;
import com.rgr.messanger.web.dto.validation.OnUpdate;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserDto {

    @NotNull(message = "ID не может быть пустым",
            groups = OnUpdate.class)
    private Long id;

    @NotBlank(message = "Никнейм не может быть пустым",
            groups = {OnCreate.class, OnUpdate.class})
    @Size(min = 3, max = 30,
            message = "Никнейм должен быть от 3 до 30 символов",
            groups = {OnCreate.class, OnUpdate.class})
    private String username;

    @NotBlank(message = "Email не может быть пустым",
            groups = {OnCreate.class, OnUpdate.class})
    @Email(message = "Некорректный формат email",
            groups = {OnCreate.class, OnUpdate.class})
    private String email;

    @NotBlank(message = "Пароль не может быть пустым",
            groups = OnCreate.class)
    @Size(min = 6, max = 255,
            message = "Пароль должен быть минимум 6 символов",
            groups = OnCreate.class)
    private String password;

    @NotBlank(message = "Подтверждение пароля не может быть пустым",
            groups = OnCreate.class)
    private String passwordConfirmation;
}