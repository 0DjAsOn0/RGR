package com.rgr.messanger.web.dto.user;

import com.rgr.messanger.web.dto.validation.OnCreate;
import com.rgr.messanger.web.dto.validation.OnUpdate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserDto {

    @NotNull(message = "ID не может быть пустым", groups = OnUpdate.class)
    private Long id;

    @NotNull(message = "Никнейм не может быть пустым", groups = {OnUpdate.class, OnCreate.class})
    @Size(min = 1, max = 255, message = "Имя должно быть от 2 до 255 символов")
    private String username;

    @NotNull(message = "Email не может быть пустым", groups = {OnUpdate.class, OnCreate.class})
    private String email;

    @NotNull(message = "пароль не может быть пустым", groups = {OnUpdate.class, OnCreate.class})
    private String password;

    @NotNull(message = "пароль не может быть пустым", groups = {OnUpdate.class, OnCreate.class})
    private String passwordConfirmation;

}
