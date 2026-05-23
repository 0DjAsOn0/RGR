package com.rgr.messanger.web.dto.chat;

import lombok.Data;

@Data
public class ChatMemberResponse {
    private Long id;
    private String username;
    private String avatarUrl;
    private String status;
    private String role;
}