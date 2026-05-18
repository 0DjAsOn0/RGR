package com.rgr.messanger.repository;

import com.rgr.messanger.entity.attachment.Attachment;
import java.util.List;

public interface AttachmentRepo {
    void save(Attachment attachment);
    List<Attachment> findByMessageId(Long messageId);
    void deleteByMessageId(Long messageId);
}