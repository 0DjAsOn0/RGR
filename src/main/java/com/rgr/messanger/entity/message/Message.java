package com.rgr.messanger.entity.message;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Message {
    private Long id;
    private String text;
    private LocalDateTime sendDate;
    private Status status;
}
