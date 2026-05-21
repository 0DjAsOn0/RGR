package com.rgr.messanger.web.dto.message;

import com.rgr.messanger.entity.attachment.Attachment;
import com.rgr.messanger.entity.message.Message;
import com.rgr.messanger.entity.message.Status;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record MessageResponse(
        Long              id,
        Long              chatId,
        Long              senderId,
        String            senderName,
        Long              replyToId,
        String            type,
        String            text,
        boolean           isEdited,
        boolean           isDeleted,
        String            time,
        String            status,
        List<Attachment>  attachments
) {
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");

    public static MessageResponse from(Message msg) {
        return from(msg, List.of());
    }

    public static MessageResponse from(Message msg, List<Attachment> attachments) {
        return new MessageResponse(
                msg.getId(),
                msg.getChatId(),
                msg.getSenderId(),
                msg.getSenderName(),
                msg.getReplyToId(),
                msg.getType(),
                msg.getText(),
                msg.isEdited(),
                msg.isDeleted(),
                formatTime(msg.getSendDate()),
                msg.getStatus() != null ? msg.getStatus().name() : Status.SENT.name(),
                attachments != null ? attachments : List.of()
        );
    }

    private static String formatTime(LocalDateTime dt) {
        return dt != null ? dt.format(TIME_FORMAT) : "";
    }
}