package com.rgr.messanger.service;

import com.rgr.messanger.entity.message.Message;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MessageService {

    Message getById(Long id);

    List<Message> getAllByUserId(Long userId);

    Message update(Message message);

    Message create(Message message, Long userId);

    void delete(Long id);
}
