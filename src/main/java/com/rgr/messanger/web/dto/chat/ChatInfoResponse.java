package com.rgr.messanger.web.dto.chat;

import lombok.Data;

@Data
public class ChatInfoResponse {
    private Long id;
    private String type;
    private Boolean isPublic;
    private String name;
    private String avatarUrl;
    private Long creatorId;
}