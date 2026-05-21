package com.rgr.messanger.service;

import com.rgr.messanger.entity.user.User;

import java.util.List;

public interface UserService {

    User getById(Long id);

    User getByUsername(String username);

    User getByEmail(String email);

    List<User> searchByUsername(String username);

    User create(User user, String passwordConfirmation);

    User update(User user);

    void updateAvatar(Long id, String avatarUrl);

    void updatePassword(Long userId, String oldPassword, String newPassword);

    void updateOnlineStatus(Long userId, boolean online);

    void updateEmailNotifications(Long userId, boolean emailNotifications);

    boolean isMessageOwner(Long userId, Long messageId);

    void delete(Long id);
}