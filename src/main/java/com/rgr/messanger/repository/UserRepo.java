package com.rgr.messanger.repository;

import com.rgr.messanger.entity.user.Role;
import com.rgr.messanger.entity.user.User;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepo {

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    void updateOnlineStatus(Long userId, boolean online);

    void verifyEmail(Long userId);

    Optional<User> findByUsername(String username);

    List<User> searchByUsername(String username);

    void updateAvatar(Long id, String avatarUrl);

    void updateUsername(Long id, String username);

    void updateEmailNotifications(Long userId, boolean emailNotifications);

    void update(User user);

    void create(User user);

    void insertUserRole(Long userId, Role role);

    boolean isMessageOwner(Long userId, Long messageId);

    void delete(Long id);

    void updatePassword(Long userId, String encodedPassword);
}
