package com.rgr.messanger.service.impl;

import com.rgr.messanger.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
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

    @Value("${app.name:Messanger}")
    private String appName;

    @Value("${spring.mail.from}")
    private String fromEmail;

    // ========================
    // ПОДТВЕРЖДЕНИЕ EMAIL
    // ========================
    @Async
    @Override
    public void sendVerificationEmail(String toEmail, String username, String token) {
        String verificationLink = baseUrl + "/api/v1/auth/verify-email?token=" + token;

        String text = """
                Привет, %s!
                
                Для подтверждения email перейдите по ссылке:
                %s
                
                Ссылка действительна 24 часа.
                """.formatted(username, verificationLink);

        sendMail(toEmail, "Подтверждение регистрации — " + appName, text);
    }

    // ========================
    // УВЕДОМЛЕНИЕ О НЕПРОЧИТАННЫХ
    // ========================
    @Async
    @Override
    public void sendUnreadNotification(String toEmail, String username,
                                       int unreadCount, String chatName) {
        String text = """
                Привет, %s!
                
                У вас %d непрочитанных сообщений в чате "%s".
                
                Войдите в мессенджер чтобы прочитать их:
                %s
                
                Если вы не хотите получать уведомления — отключите их в настройках профиля.
                """.formatted(username, unreadCount, chatName, baseUrl);

        sendMail(toEmail,
                "У вас " + unreadCount + " непрочитанных сообщений",
                text);
    }

    // ========================
    // СБРОС ПАРОЛЯ
    // ========================
    @Async
    @Override
    public void sendPasswordResetCode(String toEmail, String username, String code) {
        String text = """
                Привет, %s!
                
                Код для сброса пароля:
                
                   %s
                
                Код действует 15 минут.
                Если вы не запрашивали сброс — проигнорируйте письмо.
                """.formatted(username, code);

        sendMail(toEmail, "Сброс пароля — " + appName, text);
    }

    // ========================
    // ОБЩАЯ ОТПРАВКА
    // ========================
    private void sendMail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            log.info("Email отправлен: to={}, subject={}", to, subject);

        } catch (MailException e) {
            log.error("Ошибка отправки email на {}: {}", to, e.getMessage(), e);
            // Не пробрасываем — не блокируем основной поток
        }
    }
}