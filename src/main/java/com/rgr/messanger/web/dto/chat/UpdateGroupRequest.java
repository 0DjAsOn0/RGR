package com.rgr.messanger.web.dto.chat;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateGroupRequest {

    @Size(min = 2, max = 100, message = "Название должно быть от 2 до 100 символов")
    private String name;

    private String avatarUrl;
}