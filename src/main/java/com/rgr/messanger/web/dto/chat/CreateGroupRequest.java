package com.rgr.messanger.web.dto.chat;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Data
public class CreateGroupRequest {

    @NotBlank(message = "Название группы не может быть пустым")
    private String name;
    private List<Long> memberIds;
    private Boolean isPublic;
}