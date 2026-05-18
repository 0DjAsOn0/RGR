package com.rgr.messanger.service;

import com.rgr.messanger.entity.chat.Chat;

import java.util.List;
import java.util.Optional;

public interface ChatService {
    List<Chat> findByUserId(Long userId);
    Optional<Chat> findPrivateChat(Long userId1, Long userId2);
    Long createPrivateChat(Long creatorId);

    // для внутреннего использования (создание приватного чата)
    void addMember(Long chatId, Long userId);

    // для контроллера (только owner может добавлять)
    void addMember(Long chatId, Long userId, Long requesterId);

    List<Long> getChatMemberIds(Long chatId);
    Optional<Chat> findById(Long chatId);
    void createNotesChat(Long userId);

    // ========================
    // ГРУППОВЫЕ ЧАТЫ
    // ========================
    Long createGroupChat(String name, Long creatorId, List<Long> memberIds);
    void updateChat(Long chatId, String name, String avatarUrl, Long requesterId);
    void deleteChat(Long chatId, Long requesterId);
    void removeMember(Long chatId, Long userId, Long requesterId);
}