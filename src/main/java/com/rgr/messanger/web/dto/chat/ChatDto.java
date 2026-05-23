package com.rgr.messanger.web.dto.chat;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatDto {
    private Long id;
    private String type;
    private Boolean isPublic;
    private String name;
    private String avatarUrl;
    private String lastMessage;
    private String lastMessageType;
    private LocalDateTime lastMessageTime;
    private String interlocutorName;
    private String interlocutorAvatar;
    private Long interlocutorId;
    private int unreadCount;
}
