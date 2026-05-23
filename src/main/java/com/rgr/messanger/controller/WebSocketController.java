package com.rgr.messanger.controller;

import com.rgr.messanger.entity.message.Message;
import com.rgr.messanger.entity.message.Status;
import com.rgr.messanger.entity.user.User;
import com.rgr.messanger.repository.MessageRepo;
import com.rgr.messanger.service.ChatService;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageRepo messageRepo;
    private final UserService userService;
    private final ChatService chatService;

    // ========================
    // ОТМЕТИТЬ КАК ПРОЧИТАННОЕ
    // ========================
    @MessageMapping("/message/read")
    public void messageRead(
            @Payload Map<String, Object> body,
            Principal principal
    ) {
        if (principal == null) {
            log.warn("Unauthenticated read receipt");
            return;
        }

        try {
            Long messageId = parseLong(body.get("messageId"));
            Long chatId    = parseLong(body.get("chatId"));

            if (messageId == null || chatId == null) {
                log.warn("Invalid read receipt payload: {}", body);
                return;
            }

            User reader = userService.getByUsername(principal.getName());

            if (!chatService.isMember(chatId, reader.getId())) {
                log.warn("User {} tried to mark read in chat {} without membership",
                        reader.getId(), chatId);
                return;
            }

            messageRepo.updateStatus(messageId, Status.READ);

            messagingTemplate.convertAndSend(
                    "/topic/chat/" + chatId,
                    Optional.of(Map.of(
                            "type", "STATUS_UPDATE",
                            "messageId", messageId,
                            "status", Status.READ.name()
                    ))
            );

            log.info("Message {} marked as READ by user {}", messageId, reader.getId());

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
            @Payload Map<String, Object> body,
            Principal principal
    ) {
        if (principal == null) {
            log.warn("Unauthenticated WS message attempt to chat {}", chatId);
            return;
        }

        log.info("WS message, chatId: {}, from: {}", chatId, principal.getName());

        Message message = null;
        try {
            User sender = userService.getByUsername(principal.getName());

            if (!chatService.isMember(chatId, sender.getId())) {
                log.warn("User {} tried to send to chat {} without membership",
                        sender.getId(), chatId);
                return;
            }

            String content = body.get("content") != null
                    ? body.get("content").toString()
                    : null;

            if (content == null || content.isBlank()) return;

            Long replyToId = parseLong(body.get("replyToId"));

            message = new Message();
            message.setChatId(chatId);
            message.setSenderId(sender.getId());
            message.setSenderName(sender.getUsername());
            message.setText(content);
            message.setType("text");
            message.setReplyToId(replyToId);
            message.setStatus(Status.SENT);
            message.setSendDate(java.time.LocalDateTime.now());

            messageRepo.create(message);

            MessageResponse response = MessageResponse.from(message);

            // Рассылаем в открытый чат
            messagingTemplate.convertAndSend("/topic/chat/" + chatId, response);

            // Рассылаем всем участникам в их личные топики (для обновления списка чатов)
            List<Long> memberIds = chatService.getChatMemberIds(chatId);
            for (Long memberId : memberIds) {
                messagingTemplate.convertAndSend("/topic/user/" + memberId, response);
            }

            log.info("WS message sent, id: {}", message.getId());

        } catch (Exception e) {
            log.error("WS error: {}", e.getMessage(), e);

            if (message != null && message.getId() != null) {
                messageRepo.updateStatus(message.getId(), Status.NOT_SENDING);
            }
        }
    }

    // ========================
    // УТИЛИТЫ
    // ========================
    private Long parseLong(Object value) {
        if (value == null) return null;
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}