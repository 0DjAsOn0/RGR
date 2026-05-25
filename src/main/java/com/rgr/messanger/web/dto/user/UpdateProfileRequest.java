package com.rgr.messanger.web.dto.user;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String username;
    private String avatarUrl;
    private String oldPassword;

    // означает: Строка от начала (^) до конца ($) НЕ должна содержать [^...] русские буквы
    @Pattern(regexp = "^[^А-Яа-яЁё]+$", message = "Пароль не должен содержать русские буквы")
    private String password;

}