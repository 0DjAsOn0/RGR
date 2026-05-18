package com.rgr.messanger.service;

import com.rgr.messanger.entity.message.Message;
import com.rgr.messanger.entity.message.Status;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MessageService {

    Message getById(Long id);

    List<Message> getAllByUserId(Long userId);

    Message update(Message message);

    Message create(Message message, Long userId);

    @Transactional(readOnly = true)
    List<Message> getByChatId(Long chatId);

    // ========================
    // ОБНОВИТЬ СТАТУС СООБЩЕНИЯ
    // ========================
    @Transactional
    void updateStatus(Long messageId, Status status);

    // ========================
    // ОТМЕТИТЬ ЧАТ КАК ПРОЧИТАННЫЙ
    // ========================
    @Transactional
    void markChatAsRead(Long chatId, Long userId);

    void delete(Long id);
}
