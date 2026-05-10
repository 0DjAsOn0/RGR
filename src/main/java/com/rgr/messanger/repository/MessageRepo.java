package com.rgr.messanger.repository;

import com.rgr.messanger.entity.message.Message;
import com.rgr.messanger.entity.message.Status;

import java.util.List;
import java.util.Optional;

public interface MessageRepo {
    Optional<Message> findById(Long id);
    List<Message> findByChatId(Long chatId);       // ← новый
    List<Message> findAllByUserId(Long userId);
    void updateStatus(Long messageId, Status status);
    void create(Message message);
    void update(Message message);
    void delete(Long id);
    void assignToUserById(Long userId, Long messageId);
    void markAsRead(Long chatId, Long userId);     // ← новый
}