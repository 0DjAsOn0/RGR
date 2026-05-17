package com.rgr.messanger.service.impl;

import com.rgr.messanger.entity.user.User;
import com.rgr.messanger.exception.EmailVerificationException;
import com.rgr.messanger.repository.UserRepo;
import com.rgr.messanger.service.EmailService;
import com.rgr.messanger.service.EmailVerificationService;
import com.rgr.messanger.web.security.JwtTokenProvider;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final UserRepo userRepo;
    private final EmailService emailService;
    private final JwtTokenProvider jwtTokenProvider;

    public EmailVerificationServiceImpl(
            UserRepo userRepo,
            EmailService emailService,
            @Lazy JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepo = userRepo;
        this.emailService = emailService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public void sendVerification(String email, String username) {

        String token = jwtTokenProvider.generateVerificationToken(email);
        emailService.sendVerificationEmail(email, username, token);
    }

    @Override
    public void verifyToken(String token) {
        try {

            String email = jwtTokenProvider.getEmailFromVerificationToken(token);

            User user = userRepo.findByEmail(email)
                    .orElseThrow(() -> new EmailVerificationException("Пользователь не найден"));

            if (user.isEmailVerified()) {
                throw new EmailVerificationException("Email уже подтверждён");
            }

            userRepo.verifyEmail(user.getId());

        } catch (ExpiredJwtException e) {
            throw new EmailVerificationException("Срок действия ссылки истёк");
        } catch (JwtException e) {
            throw new EmailVerificationException("Недействительная ссылка");
        }
    }
}
