package com.rgr.messanger.service.impl;

import com.rgr.messanger.entity.user.User;
import com.rgr.messanger.exception.AccessDeniedException;
import com.rgr.messanger.exception.ResourceNotFoundException;
import com.rgr.messanger.repository.UserRepo;
import com.rgr.messanger.service.EmailService;
import com.rgr.messanger.service.PasswordResetService;
import com.rgr.messanger.service.PasswordResetStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final int CODE_LENGTH = 6;
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final SecureRandom secureRandom = new SecureRandom();

    private final UserRepo           userRepo;
    private final EmailService       emailService;
    private final PasswordResetStore resetStore;
    private final PasswordEncoder    passwordEncoder;

    // ========================
    // ОТПРАВКА КОДА
    // ========================
    @Override
    public void sendResetCode(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email обязателен");
        }

        Optional<User> userOpt = userRepo.findByEmail(email);

        // Не раскрываем существует ли email — защита от перебора
        if (userOpt.isEmpty()) {
            log.warn("Сброс пароля: email не найден {}", email);
            return;
        }

        User user = userOpt.get();
        String code = generateCode();

        resetStore.save(email, code);
        emailService.sendPasswordResetCode(email, user.getUsername(), code);

        log.info("Код сброса пароля отправлен на {}", email);
    }

    // ========================
    // ВЕРИФИКАЦИЯ КОДА
    // ========================
    @Override
    public boolean verifyCode(String email, String code) {
        if (email == null || code == null) return false;
        return resetStore.verify(email, code);
    }

    // ========================
    // СБРОС ПАРОЛЯ
    // ========================
    @Override
    @Transactional
    public void resetPassword(String email, String newPassword) {
        // 1) Проверка валидности пароля
        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Пароль должен быть не менее " + MIN_PASSWORD_LENGTH + " символов"
            );
        }

        // 2) Проверка, что email прошёл верификацию кода
        if (!resetStore.isVerified(email)) {
            throw new AccessDeniedException(
                    "Сначала подтвердите код из письма"
            );
        }

        // 3) Поиск пользователя
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        // 4) Обновление пароля + удаление кода
        userRepo.updatePassword(user.getId(), passwordEncoder.encode(newPassword));
        resetStore.remove(email);

        log.info("Пароль изменён для {}", email);
    }

    // ========================
    // PRIVATE
    // ========================

    /**
     * Генерация 6-значного кода через SecureRandom.
     * Math.random / Random — предсказуемы, использовать нельзя.
     */
    private String generateCode() {
        int code = secureRandom.nextInt(1_000_000);
        return String.format("%0" + CODE_LENGTH + "d", code);
    }
}