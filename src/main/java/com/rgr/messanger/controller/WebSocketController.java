package com.rgr.messanger.controller;

import com.rgr.messanger.entity.message.Message;
import com.rgr.messanger.entity.message.Status;
import com.rgr.messanger.entity.user.User;
import com.rgr.messanger.repository.MessageRepo;
import com.rgr.messanger.service.UserService;
import com.rgr.messanger.web.dto.message.MessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageRepo messageRepo;
    private final UserService userService;

    // ========================
    // ОТМЕТИТЬ КАК ПРОЧИТАННОЕ
    // ========================
    @MessageMapping("/message/read")
    public void messageRead(
            @Payload Map<String, Object> body,
            Principal principal
    ) {
        try {
            Long messageId = Long.valueOf(body.get("messageId").toString());
            Long chatId    = Long.valueOf(body.get("chatId").toString());

            messageRepo.updateStatus(messageId, Status.READ);

            messagingTemplate.convertAndSend(
                    "/topic/chat/" + chatId,
                    Optional.of(Map.of(
                            "type", "STATUS_UPDATE",
                            "messageId", messageId,
                            "status", Status.READ.name()
                    ))
            );

            log.info("Message {} marked as READ", messageId);

        } catch (Exception e) {
            log.error("Error marking message as read: {}", e.getMessage(), e);
        }
    }

    // ========================
    // ОТПРАВИТЬ СООБЩЕНИЕ
    // ========================
    @MessageMapping("/chat/{chatId}")
    public void sendMessage(
            @DestinationVariable Long chatId,
            @Payload Map<String, String> body,
            Principal principal
    ) {
        log.info("WS message, chatId: {}, from: {}", chatId, principal.getName());

        try {
            User sender = userService.getByUsername(principal.getName());

            String content = body.get("content");
            if (content == null || content.isBlank()) return;

            // Сохраняем сообщение
            Message message = new Message();
            message.setChatId(chatId);
            message.setSenderId(sender.getId());
            message.setText(content);
            message.setType("text");
            message.setStatus(Status.SENDING);
            messageRepo.create(message);

            // Сразу ставим SENT
            messageRepo.updateStatus(message.getId(), Status.SENT);
            message.setStatus(Status.SENT);

            // одна отправка — без дубликата
            MessageResponse response = MessageResponse.from(message);
            messagingTemplate.convertAndSend("/topic/chat/" + chatId, response);

            log.info("WS message sent, id: {}", message.getId());

        } catch (Exception e) {
            log.error("WS error: {}", e.getMessage(), e);
        }
    }
}