package com.rgr.messanger.service;

import com.rgr.messanger.entity.message.Message;
import com.rgr.messanger.entity.message.Status;
import com.rgr.messanger.web.dto.message.MessageResponse;

import java.util.List;

public interface MessageService {

    Message getById(Long id);

    List<Message> getAllByUserId(Long userId);

    List<Message> getByChatId(Long chatId);

    List<MessageResponse> getResponsesByChatId(Long chatId);

    Message create(Message message, Long userId);

    Message update(Message message);

    void updateStatus(Long messageId, Status status);

    void markChatAsRead(Long chatId, Long userId);

    void delete(Long id);
}