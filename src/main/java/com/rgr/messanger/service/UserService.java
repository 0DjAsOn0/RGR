package com.rgr.messanger.service;

import com.rgr.messanger.entity.user.User;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserService {

    User getById(Long id);

    @Transactional(readOnly = true)
    User getByUsername(String username);

    List<User> searchByUsername(String username);

    void updateEmailNotifications(Long userId, boolean emailNotifications);

    @Transactional
    void updateOnlineStatus(Long userId, boolean online);

    User getByEmail(String email);

    User update(User user);

    User create(User user);

    void updateAvatar(Long id, String avatarUrl);

    boolean isMessageOwner(Long userId, Long messageId);

    void delete(Long id);
}
