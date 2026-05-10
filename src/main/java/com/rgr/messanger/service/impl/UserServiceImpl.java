package com.rgr.messanger.service.impl;

import com.rgr.messanger.entity.user.Role;
import com.rgr.messanger.entity.user.User;
import com.rgr.messanger.exception.ResourceNotFoundException;
import com.rgr.messanger.repository.UserRepo;
import com.rgr.messanger.service.EmailVerificationService;
import com.rgr.messanger.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    @Override
    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }

    @Transactional(readOnly = true)
    @Override
    public User getByUsername(String username) {
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> searchByUsername(String username) {
        return userRepo.searchByUsername(username);
    }

    @Override
    @Transactional
    public void updateEmailNotifications(Long userId, boolean emailNotifications) {
        userRepo.updateEmailNotifications(userId, emailNotifications);
    }

    @Transactional
    @Override
    public void updateOnlineStatus(Long userId, boolean online) {
        userRepo.updateOnlineStatus(userId, online);
    }

    @Override
    @Transactional
    public User update(User user) {
        if (user.getPassword() != null
                && !user.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        userRepo.update(user);
        return user;
    }

    @Override
    @Transactional
    public void updateAvatar(Long id, String avatarUrl) {
        userRepo.updateAvatar(id, avatarUrl);
    }

    @Override
    @Transactional
    public User create(User user) {
        if (userRepo.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalStateException("Такой пользователь уже существует");
        }
        if (userRepo.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalStateException("Адрес электронной почты уже существует");
        }
        if (!user.getPassword().equals(user.getPasswordConfirmation())) {
            throw new IllegalStateException("Пароль и подтверждение пароля не совпадают");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEmailVerified(false);
        userRepo.create(user);
        Set<Role> roles = Set.of(Role.ROLE_USER);
        user.setRoles(roles);
        roles.forEach(role -> userRepo.insertUserRole(user.getId(), role));
        emailVerificationService.sendVerification(user.getEmail(), user.getUsername());

        return user;
    }

    @Override
    @Transactional
    public boolean isMessageOwner(Long userId, Long messageId) {

        return userRepo.isMessageOwner(userId, messageId);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        userRepo.delete(id);
    }
}
