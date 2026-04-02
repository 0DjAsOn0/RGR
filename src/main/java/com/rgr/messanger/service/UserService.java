package com.rgr.messanger.service;

import com.rgr.messanger.entity.user.User;

public interface UserService {

    User getById(Long id);

    User getByUsername(String username);

    User update(User user);

    User create(User user);

    boolean isMessageOwner(Long userId, Long messageId);

    void delete(Long id);
}
