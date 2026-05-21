package com.rgr.messanger.entity.chat;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Chat {
    private Long id;
    private String type;
    private String name;
    private String avatarUrl;
    private Long creatorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
