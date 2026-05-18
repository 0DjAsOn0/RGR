package com.rgr.messanger.service.impl;

import com.rgr.messanger.entity.message.Message;
import com.rgr.messanger.entity.message.Status;
import com.rgr.messanger.exception.ResourceNotFoundException;
import com.rgr.messanger.repository.MessageRepo;
import com.rgr.messanger.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepo messageRepo;

    // ========================
    // ПОЛУЧИТЬ ПО ID
    // ========================
    @Override
    @Transactional(readOnly = true)
    public Message getById(Long id) {
        return messageRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
    }

    // ========================
    // ПОЛУЧИТЬ ВСЕ СООБЩЕНИЯ ПОЛЬЗОВАТЕЛЯ
    // ========================
    @Override
    @Transactional(readOnly = true)
    public List<Message> getAllByUserId(Long userId) {
        return messageRepo.findAllByUserId(userId);
    }

    // ========================
    // ПОЛУЧИТЬ СООБЩЕНИЯ ЧАТА
    // ========================
    @Override
    @Transactional(readOnly = true)
    public List<Message> getByChatId(Long chatId) {
        return messageRepo.findByChatId(chatId);
    }

    // ========================
    // СОЗДАТЬ СООБЩЕНИЕ
    // ========================
    @Override
    @Transactional
    public Message create(Message message, Long userId) {
        // не перезаписываем статус если он уже задан
        if (message.getStatus() == null) {
            message.setStatus(Status.NOT_SENDING);
        }
        messageRepo.create(message);
        messageRepo.assignToUserById(userId, message.getId());
        return message;
    }

    // ========================
    // ОБНОВИТЬ СООБЩЕНИЕ
    // ========================
    @Override
    @Transactional
    public Message update(Message message) {
        if (message.getStatus() == null) {
            message.setStatus(Status.NOT_SENDING);
        }
        messageRepo.update(message);
        return message;
    }

    // ========================
    // ОБНОВИТЬ СТАТУС СООБЩЕНИЯ
    // ========================
    @Override
    @Transactional
    public void updateStatus(Long messageId, Status status) {
        messageRepo.updateStatus(messageId, status);
    }

    // ========================
    // ОТМЕТИТЬ ЧАТ КАК ПРОЧИТАННЫЙ
    // ========================
    @Override
    @Transactional
    public void markChatAsRead(Long chatId, Long userId) {
        messageRepo.markAsRead(chatId, userId);
    }

    // ========================
    // УДАЛИТЬ СООБЩЕНИЕ
    // ========================
    @Override
    @Transactional
    public void delete(Long id) {
        messageRepo.delete(id);
    }
}