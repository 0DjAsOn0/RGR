package com.rgr.messanger.service.impl;

import com.rgr.messanger.entity.chat.Chat;
import com.rgr.messanger.exception.AccessDeniedException;
import com.rgr.messanger.exception.ResourceNotFoundException;
import com.rgr.messanger.repository.ChatRepo;
import com.rgr.messanger.service.ChatService;
import com.rgr.messanger.web.dto.chat.ChatDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final String ROLE_OWNER  = "owner";
    private static final String ROLE_MEMBER = "member";

    private final ChatRepo chatRepo;

    // ========================
    // ПОИСК / ЧТЕНИЕ
    // ========================

    @Override
    @Transactional(readOnly = true)
    public List<ChatDto> findByUserId(Long userId) {
        return chatRepo.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Chat> findById(Long chatId) {
        return chatRepo.findById(chatId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Chat> findPrivateChat(Long userId1, Long userId2) {
        return chatRepo.findPrivateChat(userId1, userId2);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getChatMemberIds(Long chatId) {
        return chatRepo.getChatMemberIds(chatId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMember(Long chatId, Long userId) {
        return chatRepo.isMember(chatId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getMemberRole(Long chatId, Long userId) {
        return chatRepo.getMemberRole(chatId, userId);
    }

    // ========================
    // СОЗДАНИЕ
    // ========================

    @Override
    @Transactional
    public Long createPrivateChat(Long creatorId) {
        return chatRepo.createPrivateChat(creatorId);
    }

    @Override
    @Transactional
    public Long createNotesChat(Long userId) {
        return chatRepo.createNotesChat(userId);
    }

    @Override
    @Transactional
    public Long createGroupChat(String name, Long creatorId, List<Long> memberIds) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Название группы не может быть пустым");
        }

        Long chatId = chatRepo.createGroupChat(name.trim(), creatorId);

        // Создатель — owner
        chatRepo.addMemberWithRole(chatId, creatorId, ROLE_OWNER);

        // Остальные — обычные участники
        if (memberIds != null) {
            for (Long memberId : memberIds) {
                if (memberId != null && !memberId.equals(creatorId)) {
                    chatRepo.addMemberWithRole(chatId, memberId, ROLE_MEMBER);
                }
            }
        }
        return chatId;
    }

    // ========================
    // РЕДАКТИРОВАНИЕ
    // ========================

    @Override
    @Transactional
    public void updateChat(Long chatId, String name, String avatarUrl, Long requesterId) {
        requireOwner(chatId, requesterId, "редактировать группу");
        chatRepo.updateChat(chatId, name, avatarUrl);
    }

    @Override
    @Transactional
    public void deleteChat(Long chatId, Long requesterId) {
        requireOwner(chatId, requesterId, "удалить группу");
        chatRepo.deleteChat(chatId);
    }

    // ========================
    // УЧАСТНИКИ
    // ========================

    @Override
    @Transactional
    public void addMember(Long chatId, Long userId) {
        chatRepo.addMember(chatId, userId);
    }

    @Override
    @Transactional
    public void addMember(Long chatId, Long userId, Long requesterId) {
        requireOwner(chatId, requesterId, "добавлять участников");
        chatRepo.addMemberWithRole(chatId, userId, ROLE_MEMBER);
    }

    @Override
    @Transactional
    public void removeMember(Long chatId, Long userId, Long requesterId) {
        // Сам себя удалить (выйти из чата) — можно
        if (!userId.equals(requesterId)) {
            requireOwner(chatId, requesterId, "удалять участников");
        }
        chatRepo.removeMember(chatId, userId);
    }

    // ========================
    // ХЕЛПЕРЫ
    // ========================

    /**
     * Проверяет, что requesterId — владелец чата.
     * Иначе кидает AccessDeniedException.
     */
    private void requireOwner(Long chatId, Long requesterId, String action) {
        String role = chatRepo.getMemberRole(chatId, requesterId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Пользователь не является участником чата"
                ));

        if (!ROLE_OWNER.equals(role)) {
            throw new AccessDeniedException(
                    "Только владелец может " + action
            );
        }
    }
}