package com.rgr.messanger.controller;

import com.rgr.messanger.entity.chat.Chat;
import com.rgr.messanger.entity.message.Message;
import com.rgr.messanger.entity.message.Status;
import com.rgr.messanger.entity.user.User;
import com.rgr.messanger.repository.ChatRepo;
import com.rgr.messanger.repository.MessageRepo;
import com.rgr.messanger.service.UserService;
import com.rgr.messanger.web.dto.message.MessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageRepo messageRepo;
    private final ChatRepo chatRepo;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    // ========================
    // ПОЛУЧИТЬ СООБЩЕНИЯ ЧАТА
    // ========================
    @GetMapping("/chat/{chatId}")
    @ResponseBody
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable Long chatId,
            Principal principal
    ) {
        log.info("getMessages called, chatId: {}, principal: {}",
                chatId, principal != null ? principal.getName() : "NULL");

        if (principal == null) return ResponseEntity.status(401).build();

        try {
            User me = userService.getByUsername(principal.getName());
            log.info("User found: {}", me.getUsername());

            List<Message> messages = messageRepo.findByChatId(chatId);
            log.info("Messages found: {}", messages.size());

            messageRepo.markAsRead(chatId, me.getId());

            List<MessageResponse> response = messages.stream()
                    .map(MessageResponse::from)
                    .toList();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting messages: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    // ========================
    // ОТПРАВИТЬ СООБЩЕНИЕ (HTTP fallback)
    // ========================
    @PostMapping("/chat/{chatId}")
    @ResponseBody
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable Long chatId,
            @RequestBody Map<String, String> body,
            Principal principal
    ) {
        if (principal == null) return ResponseEntity.status(401).build();

        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            User me = userService.getByUsername(principal.getName());

            Message message = new Message();
            message.setChatId(chatId);
            message.setSenderId(me.getId());
            message.setText(content);
            message.setType("text");
            message.setStatus(Status.SENDING);
            messageRepo.create(message);

            // Сразу ставим SENT
            messageRepo.updateStatus(message.getId(), Status.SENT);
            message.setStatus(Status.SENT);

            MessageResponse response = MessageResponse.from(message);

            // Рассылаем через WS
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + chatId,
                    response
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error sending message: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    // ========================
    // ОТМЕТИТЬ КАК ПРОЧИТАННОЕ (HTTP)
    // ========================
    @PostMapping("/{messageId}/read")
    @ResponseBody
    public ResponseEntity<Void> markRead(
            @PathVariable Long messageId,
            Principal principal
    ) {
        if (principal == null) return ResponseEntity.status(401).build();

        try {
            messageRepo.updateStatus(messageId, Status.READ);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error marking as read: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    // ========================
    // СОЗДАТЬ ИЛИ НАЙТИ ПРИВАТНЫЙ ЧАТ
    // ========================
    @PostMapping("/private/{userId}")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> getOrCreatePrivateChat(
            @PathVariable Long userId,
            Principal principal
    ) {
        if (principal == null) return ResponseEntity.status(401).build();

        try {
            User me = userService.getByUsername(principal.getName());

            Optional<Chat> existing = chatRepo.findPrivateChat(me.getId(), userId);
            if (existing.isPresent()) {
                return ResponseEntity.ok(Map.of("chatId", existing.get().getId()));
            }

            Long chatId = chatRepo.createPrivateChat(me.getId());
            chatRepo.addMember(chatId, me.getId());
            chatRepo.addMember(chatId, userId);

            return ResponseEntity.ok(Map.of("chatId", chatId));

        } catch (Exception e) {
            log.error("Error creating chat: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
}