package com.rgr.messanger.service.impl;

import com.rgr.messanger.entity.chat.Chat;
import com.rgr.messanger.repository.ChatRepo;
import com.rgr.messanger.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepo chatRepo;

    @Override
    @Transactional(readOnly = true)
    public List<Chat> findByUserId(Long userId) {
        return chatRepo.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Chat> findPrivateChat(Long userId1, Long userId2) {
        return chatRepo.findPrivateChat(userId1, userId2);
    }

    @Override
    @Transactional
    public Long createPrivateChat(Long creatorId) {
        return chatRepo.createPrivateChat(creatorId);
    }

    @Override
    @Transactional
    public void addMember(Long chatId, Long userId) {
        chatRepo.addMember(chatId, userId);
    }

    @Transactional
    @Override
    public void addMember(Long chatId, Long userId, Long requesterId) {
        String role = chatRepo.getMemberRole(chatId, requesterId);
        if (!"owner".equals(role)) {
            throw new IllegalStateException("Только владелец может добавлять участников");
        }
        chatRepo.addMemberWithRole(chatId, userId, "member");
    }

    @Transactional(readOnly = true)
    @Override
    public List<Long> getChatMemberIds(Long chatId) {
        return chatRepo.getChatMemberIds(chatId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Chat> findById(Long chatId) {
        return chatRepo.findById(chatId);
    }

    @Override
    @Transactional
    public void createNotesChat(Long userId) {
        chatRepo.createNotesChat(userId);
    }


    @Override
    @Transactional
    public Long createGroupChat(String name, Long creatorId, List<Long> memberIds) {
        if (name == null || name.isBlank()) {
            throw new IllegalStateException("Название группы не может быть пустым");
        }

        Long chatId = chatRepo.createGroupChat(name, creatorId);

        // добавляем создателя как owner
        chatRepo.addMemberWithRole(chatId, creatorId, "owner");

        // добавляем остальных участников
        for (Long memberId : memberIds) {
            if (!memberId.equals(creatorId)) {
                chatRepo.addMemberWithRole(chatId, memberId, "member");
            }
        }
        return chatId;
    }

    @Override
    @Transactional
    public void updateChat(Long chatId, String name, String avatarUrl, Long requesterId) {
        String role = chatRepo.getMemberRole(chatId, requesterId);
        if (!"owner".equals(role)) {
            throw new IllegalStateException("Только владелец может редактировать группу");
        }
        chatRepo.updateChat(chatId, name, avatarUrl);
    }

    @Override
    @Transactional
    public void deleteChat(Long chatId, Long requesterId) {
        String role = chatRepo.getMemberRole(chatId, requesterId);
        if (!"owner".equals(role)) {
            throw new IllegalStateException("Только владелец может удалить группу");
        }
        chatRepo.deleteChat(chatId);
    }

    @Override
    @Transactional
    public void removeMember(Long chatId, Long userId, Long requesterId) {
        String role = chatRepo.getMemberRole(chatId, requesterId);
        if (!"owner".equals(role)) {
            throw new IllegalStateException("Только владелец может удалять участников");
        }
        chatRepo.removeMember(chatId, userId);
    }
}