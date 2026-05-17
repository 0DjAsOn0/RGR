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
}