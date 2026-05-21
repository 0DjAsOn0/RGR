package com.rgr.messanger.service;

import com.rgr.messanger.entity.chat.Chat;
import com.rgr.messanger.web.dto.chat.ChatDto;

import java.util.List;
import java.util.Optional;

public interface ChatService {

    List<ChatDto> findByUserId(Long userId);

    Optional<Chat> findById(Long chatId);

    Optional<Chat> findPrivateChat(Long userId1, Long userId2);

    Long createPrivateChat(Long creatorId);

    Long createNotesChat(Long userId);

    Long createGroupChat(String name, Long creatorId, List<Long> memberIds);

    void updateChat(Long chatId, String name, String avatarUrl, Long requesterId);

    void deleteChat(Long chatId, Long requesterId);

    List<Long> getChatMemberIds(Long chatId);

    boolean isMember(Long chatId, Long userId);

    void addMember(Long chatId, Long userId);

    void addMember(Long chatId, Long userId, Long requesterId);

    void removeMember(Long chatId, Long userId, Long requesterId);

    Optional<String> getMemberRole(Long chatId, Long userId);
}