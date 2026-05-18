package com.rgr.messanger.repository;

import com.rgr.messanger.entity.chat.Chat;

import java.util.List;
import java.util.Optional;

public interface ChatRepo {
    List<Chat> findByUserId(Long userId);           // все чаты пользователя

    void createNotesChat(Long userId);

    Optional<Chat> findPrivateChat(Long userId1, Long userId2); // приватный чат между двумя

    List<Long> getChatMemberIds(Long chatId);

    Long createPrivateChat(Long creatorId);         // создать чат
    void addMember(Long chatId, Long userId);       // добавить участника
    Optional<Chat> findById(Long chatId);

    // ========================
    // ГРУППОВЫЕ ЧАТЫ
    // ========================

    Long createGroupChat(String name, Long creatorId);
    void updateChat(Long chatId, String name, String avatarUrl);
    void deleteChat(Long chatId);
    void removeMember(Long chatId, Long userId);
    String getMemberRole(Long chatId, Long userId);

    void addMemberWithRole(Long chatId, Long userId, String role);
}
