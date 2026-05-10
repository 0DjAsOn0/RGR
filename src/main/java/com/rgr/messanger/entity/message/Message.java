package com.rgr.messanger.entity.message;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Message {
    private Long id;
    private Long chatId;
    private Long senderId;
    private Long replyToId;
    private String type;
    private String text;
    private boolean edited;
    private boolean deleted;
    private LocalDateTime sendDate;
    private LocalDateTime editedAt;
    private Status status;
}
