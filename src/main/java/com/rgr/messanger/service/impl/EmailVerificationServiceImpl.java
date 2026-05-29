package com.rgr.messanger.service.impl;

import com.rgr.messanger.entity.user.User;
import com.rgr.messanger.exception.EmailVerificationException;
import com.rgr.messanger.repository.UserRepo;
import com.rgr.messanger.service.EmailService;
import com.rgr.messanger.service.EmailVerificationService;
import com.rgr.messanger.web.security.JwtTokenProvider;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final UserRepo         userRepo;
    private final EmailService     emailService;
    private final JwtTokenProvider jwtTokenProvider;


    public EmailVerificationServiceImpl(
            UserRepo userRepo,
            EmailService emailService,
            @Lazy JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepo         = userRepo;
        this.emailService     = emailService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    //отправка письма с верификацией -> далле зайти в сервис
    @Override
    public void sendVerification(String email, String username) {
        String token = jwtTokenProvider.generateVerificationToken(email);
        emailService.sendVerificationEmail(email, username, token);
        log.info("Verification email queued for: {}", email);
    }

    //подтверждение
    @Override
    @Transactional
    public void verifyToken(String token) {

        //проверка что токен есть
        if (token == null || token.isBlank()) {
            throw new EmailVerificationException("Токен не указан");
        }

        //извлекаем емаил из токена
        String email;
        try {
            email = jwtTokenProvider.getEmailFromVerificationToken(token);
        } catch (ExpiredJwtException e) {
            throw new EmailVerificationException("Срок действия ссылки истёк");
        } catch (JwtException | IllegalArgumentException e) {
            throw new EmailVerificationException("Недействительная ссылка");
        }

        //ищем в бд пользователя с такой почтой
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new EmailVerificationException("Пользователь не найден"));

        //подтверждается только один раз
        if (user.isEmailVerified()) {
            throw new EmailVerificationException("Email уже подтверждён");
        }

        userRepo.verifyEmail(user.getId());
        log.info("Email подтверждён: {}", email);
    }
}