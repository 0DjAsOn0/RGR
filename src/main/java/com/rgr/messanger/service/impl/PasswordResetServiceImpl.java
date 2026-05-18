package com.rgr.messanger.service.impl;

import com.rgr.messanger.entity.user.User;
import com.rgr.messanger.repository.UserRepo;
import com.rgr.messanger.service.EmailService;
import com.rgr.messanger.service.PasswordResetService;
import com.rgr.messanger.service.PasswordResetStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepo userRepo;
    private final EmailService emailService;
    private final PasswordResetStore resetStore;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void sendResetCode(String email) {
        Optional<User> userOpt = userRepo.findByEmail(email);

        if (userOpt.isEmpty()) {
            log.warn("Сброс пароля: email не найден {}", email);
            return;
        }

        String code = String.format("%06d", new Random().nextInt(999999));
        resetStore.save(email, code);
        emailService.sendPasswordResetCode(
                email,
                userOpt.get().getUsername(),
                code
        );

        log.info("Код сброса пароля отправлен на {}", email);
    }

    @Override
    public boolean verifyCode(String email, String code) {
        return resetStore.verify(email, code);
    }

    @Override
    public void resetPassword(String email, String newPassword) {
        Optional<User> userOpt = userRepo.findByEmail(email);

        if (userOpt.isEmpty()) {
            throw new RuntimeException("Пользователь не найден");
        }

        userRepo.updatePassword(
                userOpt.get().getId(),
                passwordEncoder.encode(newPassword)
        );
        resetStore.remove(email);

        log.info("Пароль изменён для {}", email);
    }
}