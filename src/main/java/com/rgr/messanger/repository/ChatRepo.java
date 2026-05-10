package com.rgr.messanger.repository;

import com.rgr.messanger.entity.chat.Chat;

import java.util.List;
import java.util.Optional;

public interface ChatRepo {
    List<Chat> findByUserId(Long userId);           // все чаты пользователя
    Optional<Chat> findPrivateChat(Long userId1, Long userId2); // приватный чат между двумя
    Long createPrivateChat(Long creatorId);         // создать чат
    void addMember(Long chatId, Long userId);       // добавить участника
    Optional<Chat> findById(Long chatId);
}
