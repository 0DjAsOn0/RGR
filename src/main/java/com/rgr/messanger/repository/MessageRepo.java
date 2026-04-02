package com.rgr.messanger.repository;

import com.rgr.messanger.entity.message.Message;

import java.util.List;
import java.util.Optional;

public interface MessageRepo {

    Optional<Message> findById(Long id);

    List<Message> findAllByUserId(Long userId);

    void assignToUserById(Long userId, Long messageId);

    void create(Message message);

    void delete(Long id);

    void update(Message message);
}
