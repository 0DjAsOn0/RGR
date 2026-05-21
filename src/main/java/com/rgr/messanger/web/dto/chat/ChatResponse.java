package com.rgr.messanger.web.dto.chat;

import java.time.format.DateTimeFormatter;

public record ChatResponse(
        Long    id,
        String  type,
        String  name,
        String  avatarUrl,
        String  lastMessage,
        String  lastMessageType,
        boolean hasAttachment,
        String  lastMessageTime,
        Long    interlocutorId,
        String  interlocutorName,
        String  interlocutorAvatar,
        int     unreadCount
) {
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");

    public static ChatResponse from(ChatDto dto) {
        String type = dto.getLastMessageType();
        boolean hasAttach = type != null && !"text".equals(type);

        String time = dto.getLastMessageTime() != null
                ? dto.getLastMessageTime().format(TIME_FORMAT)
                : "";

        return new ChatResponse(
                dto.getId(),
                dto.getType(),
                dto.getName(),
                dto.getAvatarUrl(),
                dto.getLastMessage(),
                type,
                hasAttach,
                time,
                dto.getInterlocutorId(),
                dto.getInterlocutorName(),
                dto.getInterlocutorAvatar(),
                dto.getUnreadCount()
        );
    }
}