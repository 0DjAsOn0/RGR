package com.rgr.messanger.entity.attachment;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Attachment {
    private Long id;
    private Long messageId;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private LocalDateTime createdAt;
}