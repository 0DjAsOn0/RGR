package com.rgr.messanger.service;

import com.rgr.messanger.entity.chat.Chat;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ChatService {
    List<Chat> findByUserId(Long userId);
    Optional<Chat> findPrivateChat(Long userId1, Long userId2);
    Long createPrivateChat(Long creatorId);
    void addMember(Long chatId, Long userId);

    @Transactional(readOnly = true)
    List<Long> getChatMemberIds(Long chatId);

    Optional<Chat> findById(Long chatId);
    void createNotesChat(Long userId);
}