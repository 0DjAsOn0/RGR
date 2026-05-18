package com.rgr.messanger.web.dto.message;

import com.rgr.messanger.entity.message.Message;
import com.rgr.messanger.entity.message.Status;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record MessageResponse(
        Long id,
        Long chatId,
        Long senderId,
        String senderName,
        String text,
        String time,
        String status
) {
    public static MessageResponse from(Message msg) {
        String time = msg.getSendDate() != null
                ? msg.getSendDate().format(DateTimeFormatter.ofPattern("HH:mm"))
                : LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        return new MessageResponse(
                msg.getId(),
                msg.getChatId(),
                msg.getSenderId(),
                msg.getSenderName(),
                msg.getText(),
                time,
                msg.getStatus() != null
                        ? msg.getStatus().name()
                        : Status.SENT.name()
        );
    }
}