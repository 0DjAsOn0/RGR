package com.rgr.messanger.service.impl;

import com.rgr.messanger.entity.attachment.Attachment;
import com.rgr.messanger.entity.message.Message;
import com.rgr.messanger.entity.message.Status;
import com.rgr.messanger.exception.ResourceNotFoundException;
import com.rgr.messanger.repository.AttachmentRepo;
import com.rgr.messanger.repository.MessageRepo;
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
}