package com.rgr.messanger.service.impl;

import com.rgr.messanger.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.from}")
    private String fromEmail;

    @Async
    @Override
    public void sendVerificationEmail(String toEmail, String username, String token) {
        log.info("Preparing email for: {}", toEmail);
        log.info("Base URL: {}", baseUrl);

        String verificationLink = baseUrl + "/api/v1/auth/verify-email?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Подтверждение регистрации");
        message.setText("""
            Привет, %s!
            
            Для подтверждения email перейдите по ссылке:
            %s
            
            Ссылка действительна 24 часа.
            """.formatted(username, verificationLink));

        log.info("Sending email...");
        mailSender.send(message);
        log.info("Email sent successfully to: {}", toEmail);
    }

//    ========================
//    ОТПРАВКА СООБЩЕНИЯ НА ПОЧТУ КОГДА 10 НЕПРОЧИТАНЫХ СООБЩЕНИЙ В ЧАте
//    ===========================

    @Async
    @Override
    public void sendUnreadNotification(String toEmail, String username,
                                       int unreadCount, String chatName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("У вас " + unreadCount + " непрочитанных сообщений");
            message.setText("""
            Привет, %s!
            
            У вас %d непрочитанных сообщений в чате "%s".
            
            Войдите в мессенджер чтобы прочитать их:
            %s
            
            Если вы не хотите получать уведомления — отключите их в настройках профиля.
            """.formatted(username, unreadCount, chatName, baseUrl));

            mailSender.send(message);
            log.info("Уведомление отправлено на: {}", toEmail);
        } catch (Exception e) {
            log.error("Ошибка отправки уведомления: {}", e.getMessage());
        }
    }


    @Override
    public void sendPasswordResetCode(String to, String username, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Сброс пароля — sicelica");
        message.setText("""
            Привет, %s!
            
            Код для сброса пароля:
            
               %s
            
            Код действует 15 минут.
            Если вы не запрашивали сброс — проигнорируйте письмо.
            """.formatted(username, code));
        mailSender.send(message);
    }

}