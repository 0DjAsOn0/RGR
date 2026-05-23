package com.rgr.messanger.service;

import com.rgr.messanger.entity.chat.Chat;
import com.rgr.messanger.web.dto.chat.ChatDto;
import com.rgr.messanger.web.dto.chat.ChatInfoResponse;
import com.rgr.messanger.web.dto.chat.ChatMemberResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface ChatService {

    // ========================
    // ПОИСК / ЧТЕНИЕ
    // ========================

    List<ChatDto> findByUserId(Long userId);

    Optional<Chat> findById(Long chatId);

    Optional<Chat> findPrivateChat(Long userId1, Long userId2);

    List<Long> getChatMemberIds(Long chatId);

    boolean isMember(Long chatId, Long userId);

    Optional<String> getMemberRole(Long chatId, Long userId);

    ChatInfoResponse getChatInfo(Long chatId, Long userId);

    List<ChatMemberResponse> getChatMembers(Long chatId, Long userId);

    // ========================
    // СОЗДАНИЕ
    // ========================

    Long createPrivateChat(Long creatorId);

    Long createNotesChat(Long userId);

    @Transactional
    Long createGroupChat(String name, Long creatorId, List<Long> memberIds, Boolean isPublic);

    // ========================
    // РЕДАКТИРОВАНИЕ
    // ========================

    void updateChat(Long chatId, String name, String avatarUrl, Long requesterId);

    void updateChatName(Long chatId, String name, Long currentUserId);

    String updateChatAvatar(Long chatId, MultipartFile file, Long currentUserId);

    void deleteChat(Long chatId, Long requesterId);

    // ========================
    // УЧАСТНИКИ
    // ========================

    void addMember(Long chatId, Long userId);

    void addMember(Long chatId, Long userId, Long currentUserId);

    void removeMember(Long chatId, Long userId, Long currentUserId);

    List<ChatDto> searchConversations(String query);

    void updateChatPrivacy(Long chatId, Boolean isPublic, Long requesterId);

    void joinPublicGroup(Long chatId, Long userId);
}