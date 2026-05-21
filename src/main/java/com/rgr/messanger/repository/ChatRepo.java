package com.rgr.messanger.repository;

import com.rgr.messanger.entity.chat.Chat;
import com.rgr.messanger.web.dto.chat.ChatDto;

import java.util.List;
import java.util.Optional;

public interface ChatRepo {

    List<ChatDto> findByUserId(Long userId);

    Optional<Chat> findById(Long chatId);

    Optional<Chat> findPrivateChat(Long userId1, Long userId2);

    Long createPrivateChat(Long creatorId);

    Long createNotesChat(Long userId);

    Long createGroupChat(String name, Long creatorId);

    void updateChat(Long chatId, String name, String avatarUrl);

    void deleteChat(Long chatId);

    List<Long> getChatMemberIds(Long chatId);

    boolean isMember(Long chatId, Long userId);

    void addMember(Long chatId, Long userId);

    void addMemberWithRole(Long chatId, Long userId, String role);

    void removeMember(Long chatId, Long userId);

    Optional<String> getMemberRole(Long chatId, Long userId);
}