package com.rgr.messanger.notification;

import com.rgr.messanger.repository.MessageRepo;
import com.rgr.messanger.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final MessageRepo messageRepo;
    private final EmailService emailService;

    //каждые 3 минуты проверяем непрочитанные
    @Scheduled(fixedDelay = 3 * 60 * 1000)
    public void sendUnreadNotifications() {
        log.info("Проверка непрочитанных сообщений...");

        try {
            List<Map<String, Object>> chats =
                    messageRepo.findChatsWithUnreadThreshold();

            for (Map<String, Object> row : chats) {
                String email     = (String) row.get("user_email");
                String username  = (String) row.get("user_username");
                int unreadCount  = ((Number) row.get("unread_count")).intValue();
                String chatName  = (String) row.get("chat_name");

                // для приватных чатов chatName может быть null
                if (chatName == null || chatName.isBlank()) {
                    chatName = "личный чат";
                }

                emailService.sendUnreadNotification(
                        email, username, unreadCount, chatName
                );
            }

            log.info("Отправлено {} уведомлений", chats.size());

        } catch (Exception e) {
            log.error("Ошибка планировщика: {}", e.getMessage(), e);
        }
    }
}