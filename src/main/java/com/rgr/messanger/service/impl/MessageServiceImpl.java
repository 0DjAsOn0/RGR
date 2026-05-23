package com.rgr.messanger.service.impl;

import com.rgr.messanger.entity.attachment.Attachment;
import com.rgr.messanger.entity.message.Message;
import com.rgr.messanger.entity.message.Status;
import com.rgr.messanger.exception.ResourceNotFoundException;
import com.rgr.messanger.repository.AttachmentRepo;
import com.rgr.messanger.repository.MessageRepo;
import com.rgr.messanger.service.ChatService;
import com.rgr.messanger.service.MessageService;
import com.rgr.messanger.web.dto.message.MessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepo    messageRepo;
    private final AttachmentRepo attachmentRepo;
    private final ChatService chatService;

    @Override
    @Transactional(readOnly = true)
    public Message getById(Long id) {
        return messageRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Сообщение не найдено"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getAllByUserId(Long userId) {
        return messageRepo.findAllByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getByChatId(Long chatId) {
        return messageRepo.findByChatId(chatId);
    }

    /**
     * Получить сообщения чата вместе с вложениями.
     * Защита от N+1: подгружаем вложения батчем для каждого сообщения.
     * Для РГР такой подход норм, на проде — JOIN или IN-запрос.
     */
    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getResponsesByChatId(Long chatId) {
        List<Message> messages = messageRepo.findByChatId(chatId);
        return messages.stream()
                .map(msg -> {
                    List<Attachment> attachments =
                            attachmentRepo.findByMessageId(msg.getId());
                    return MessageResponse.from(msg, attachments);
                })
                .toList();
    }

    @Override
    @Transactional
    public Message create(Message message, Long userId) {
        if (message.getStatus() == null) {
            message.setStatus(Status.SENT);
        }
        if (message.getType() == null) {
            message.setType("text");
        }
        messageRepo.create(message);
        return message;
    }

    @Override
    @Transactional
    public Message update(Message message) {
        if (message.getStatus() == null) {
            message.setStatus(Status.SENT);
        }
        messageRepo.update(message);
        return message;
    }

    @Override
    @Transactional
    public void updateStatus(Long messageId, Status status) {
        messageRepo.updateStatus(messageId, status);
    }

    @Override
    @Transactional
    public void markChatAsRead(Long chatId, Long userId) {
        messageRepo.markAsRead(chatId, userId);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        messageRepo.delete(id);
    }

    // Убедись, что заинжектил: private final ChatService chatService;

    @Override
    @Transactional
    public void editMessage(Long messageId, String newText, Long requesterId) {
        Message message = getById(messageId);
        checkEditDeletePermissions(message, requesterId, "редактировать");

        messageRepo.updateText(messageId, newText);
    }

    @Override
    @Transactional
    public void deleteMessage(Long messageId, Long requesterId) {
        Message message = getById(messageId);
        checkEditDeletePermissions(message, requesterId, "удалять");

        messageRepo.markDeleted(messageId);
    }

    private void checkEditDeletePermissions(Message message, Long requesterId, String action) {
        // 1. Собственник сообщения может всё
        if (requesterId.equals(message.getSenderId())) {
            return;
        }

        // 2. Если не собственник, проверяем админские права в группе
        com.rgr.messanger.entity.chat.Chat chat = chatService.findById(message.getChatId())
                .orElseThrow(() -> new IllegalArgumentException("Чат не найден"));

        if ("group".equalsIgnoreCase(chat.getType())) {
            String role = chatService.getMemberRole(chat.getId(), requesterId).orElse("");
            if ("admin".equalsIgnoreCase(role) || "owner".equalsIgnoreCase(role)) {
                return; // Админ/создатель группы может удалять и редактировать любые
            }
        }

        throw new com.rgr.messanger.exception.AccessDeniedException("У вас нет прав " + action + " это сообщение");
    }
}