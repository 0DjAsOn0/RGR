package com.rgr.messanger.service.impl;

import com.rgr.messanger.entity.chat.Chat;
import com.rgr.messanger.exception.AccessDeniedException;
import com.rgr.messanger.exception.ResourceNotFoundException;
import com.rgr.messanger.repository.ChatRepo;
import com.rgr.messanger.repository.MessageRepo;
import com.rgr.messanger.service.ChatService;
import com.rgr.messanger.service.FileStorageService;
import com.rgr.messanger.web.dto.chat.ChatDto;
import com.rgr.messanger.web.dto.chat.ChatInfoResponse;
import com.rgr.messanger.web.dto.chat.ChatMemberResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final String ROLE_OWNER = "owner";
    private static final String ROLE_MEMBER = "member";
    private static final String TYPE_GROUP = "group";

    private final ChatRepo chatRepo;
    private final FileStorageService fileStorageService;
    private SimpMessagingTemplate messagingTemplate;
    private final MessageRepo messageRepo;

    @Autowired
    public void setMessagingTemplate(@Lazy SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

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

    @Override
    @Transactional(readOnly = true)
    public ChatInfoResponse getChatInfo(Long chatId, Long userId) {
        if (!chatRepo.isMember(chatId, userId)) {
            throw new AccessDeniedException("Вы не являетесь участником чата");
        }

        Chat chat = chatRepo.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Чат не найден"));

        ChatInfoResponse dto = new ChatInfoResponse();
        dto.setId(chat.getId());
        dto.setType(chat.getType());
        dto.setName(chat.getName());
        dto.setAvatarUrl(chat.getAvatarUrl());
        dto.setCreatorId(chat.getCreatorId());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMemberResponse> getChatMembers(Long chatId, Long userId) {
        if (!chatRepo.isMember(chatId, userId)) {
            throw new AccessDeniedException("Вы не являетесь участником чата");
        }
        return chatRepo.getMembersDetailed(chatId);
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

        chatRepo.addMemberWithRole(chatId, creatorId, ROLE_OWNER);

        if (memberIds != null) {
            for (Long memberId : memberIds) {
                if (memberId != null && !memberId.equals(creatorId)) {
                    chatRepo.addMemberWithRole(chatId, memberId, ROLE_MEMBER);
                    notifyChatListUpdate(memberId, chatId, "added");
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
        notifyAllMembersChatUpdated(chatId);
    }

    @Override
    @Transactional
    public void updateChatName(Long chatId, String name, Long currentUserId) {
        Chat chat = requireGroupChat(chatId);
        requireCreator(chat, currentUserId);

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Название не может быть пустым");
        }

        chatRepo.updateChat(chatId, name.trim(), null);
        notifyAllMembersChatUpdated(chatId);
    }

    @Override
    @Transactional
    public String updateChatAvatar(Long chatId, MultipartFile file, Long currentUserId) {
        Chat chat = requireGroupChat(chatId);
        requireCreator(chat, currentUserId);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл не передан");
        }

        String url = fileStorageService.store(file, "avatars");
        chatRepo.updateChat(chatId, null, url);
        notifyAllMembersChatUpdated(chatId);
        return url;
    }

    @Override
    @Transactional
    public void deleteChat(Long chatId, Long requesterId) {
        Chat chat = chatRepo.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Чат не найден"));

        if ("group".equalsIgnoreCase(chat.getType())) {
            requireOwner(chatId, requesterId, "удалить группу");
        } else {
            if (!chatRepo.isMember(chatId, requesterId)) {
                throw new AccessDeniedException("Вы не являетесь участником чата");
            }
        }

        List<Long> members = chatRepo.getChatMemberIds(chatId);

        boolean isNotesChat = ("private".equalsIgnoreCase(chat.getType()) && members.size() == 1)
                || "notes".equalsIgnoreCase(chat.getType());

        if (isNotesChat) {

            messageRepo.markDeletedByChatId(chatId);

            for (Long memberId : members) {
                notifyChatListUpdate(memberId, chatId, "notes_cleared");
            }
            return;
        }

        chatRepo.deleteChat(chatId);

        for (Long memberId : members) {
            notifyChatListUpdate(memberId, chatId, "deleted");
        }
    }

    // ========================
    // УЧАСТНИКИ
    // ========================

    @Override
    @Transactional
    public void addMember(Long chatId, Long userId) {
        chatRepo.addMember(chatId, userId);
        notifyChatListUpdate(userId, chatId, "added");
    }

    @Override
    @Transactional
    public void addMember(Long chatId, Long userId, Long currentUserId) {
        Chat chat = requireGroupChat(chatId);
        requireCreator(chat, currentUserId);

        if (userId == null) {
            throw new IllegalArgumentException("userId не передан");
        }

        if (chatRepo.isMember(chatId, userId)) {
            throw new IllegalArgumentException("Пользователь уже в чате");
        }

        chatRepo.addMemberWithRole(chatId, userId, ROLE_MEMBER);
        notifyChatListUpdate(userId, chatId, "added");
        notifyAllMembersChatUpdated(chatId);
    }

    @Override
    @Transactional
    public void removeMember(Long chatId, Long userId, Long currentUserId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId не передан");
        }

        if (userId.equals(currentUserId)) {
            if (!chatRepo.isMember(chatId, userId)) {
                throw new IllegalArgumentException("Пользователь не состоит в чате");
            }
            chatRepo.removeMember(chatId, userId);
            notifyChatListUpdate(userId, chatId, "removed");
            notifyAllMembersChatUpdated(chatId);
            return;
        }

        Chat chat = requireGroupChat(chatId);
        requireCreator(chat, currentUserId);

        if (userId.equals(chat.getCreatorId())) {
            throw new IllegalArgumentException("Нельзя удалить создателя беседы");
        }

        if (!chatRepo.isMember(chatId, userId)) {
            throw new IllegalArgumentException("Пользователь не состоит в чате");
        }

        chatRepo.removeMember(chatId, userId);
        notifyChatListUpdate(userId, chatId, "removed");
        notifyAllMembersChatUpdated(chatId);
    }

    // ========================
    // WS-уведомления
    // ========================

    private void notifyChatListUpdate(Long userId, Long chatId, String action) {
        if (userId == null || messagingTemplate == null) return;
        try {
            messagingTemplate.convertAndSend(
                    "/topic/user/" + userId,
                    Optional.of(Map.of(
                            "type", "CHAT_LIST_UPDATE",
                            "action", action,
                            "chatId", chatId
                    ))
            );
        } catch (Exception ignored) {
        }
    }

    private void notifyAllMembersChatUpdated(Long chatId) {
        List<Long> members = chatRepo.getChatMemberIds(chatId);
        for (Long memberId : members) {
            notifyChatListUpdate(memberId, chatId, "updated");
        }
    }

    // ========================
    // ХЕЛПЕРЫ
    // ========================

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

    private Chat requireGroupChat(Long chatId) {
        Chat chat = chatRepo.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Чат не найден"));

        if (!TYPE_GROUP.equalsIgnoreCase(chat.getType())) {
            throw new IllegalArgumentException("Операция доступна только для групп");
        }
        return chat;
    }

    private void requireCreator(Chat chat, Long userId) {
        if (chat.getCreatorId() == null || !chat.getCreatorId().equals(userId)) {
            throw new AccessDeniedException(
                    "Только создатель беседы может это делать"
            );
        }
    }
}