package com.rgr.messanger.web.dto.message;

import com.rgr.messanger.entity.message.Status;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MessageDto {
    private Long id;
    private String text;
    private LocalDateTime sendDate;
    private Status status;
}
