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

    @Override
    @Transactional(readOnly = true)
    public Message getById(Long id) {
        return messageRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getAllByUserId(Long userId) {
        return messageRepo.findAllByUserId(userId);
    }

    @Override
    @Transactional
    public Message update(Message message) {
        if(message.getStatus() == null){
            message.setStatus(Status.NOT_SENDING);
        }
        messageRepo.update(message);
        return message;
    }

    @Transactional
    @Override
    public Message create(Message message, Long userId) {
        message.setStatus(Status.NOT_SENDING);
        messageRepo.create(message);
        messageRepo.assignToUserById(message.getId(), userId);
        return message;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        messageRepo.delete(id);
    }
}
