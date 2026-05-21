package com.rgr.messanger.controller;

import com.rgr.messanger.entity.chat.Chat;
import com.rgr.messanger.entity.message.Message;
import com.rgr.messanger.entity.message.Status;
import com.rgr.messanger.exception.AccessDeniedException;
import com.rgr.messanger.service.ChatService;
import com.rgr.messanger.service.MessageService;
import com.rgr.messanger.web.dto.message.MessageResponse;
import com.rgr.messanger.web.security.JwtEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService       messageService;
    private final ChatService          chatService;
    private final SimpMessagingTemplate messagingTemplate;

    // ========================
    // ПОЛУЧИТЬ СООБЩЕНИЯ ЧАТА
    // ========================
    @GetMapping("/chat/{chatId}")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable Long chatId,
            @AuthenticationPrincipal JwtEntity user
    ) {
        ensureMember(chatId, user.getId());
        return ResponseEntity.ok(messageService.getResponsesByChatId(chatId));
    }

    // ========================
    // ОТПРАВИТЬ СООБЩЕНИЕ (HTTP fallback при отключённом WS)
    // ========================
    @PostMapping("/chat/{chatId}")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable Long chatId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal JwtEntity user
    ) {
        ensureMember(chatId, user.getId());

        String content = body.get("content") != null
                ? body.get("content").toString()
                : null;

        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Long replyToId = parseLong(body.get("replyToId"));

        Message message = new Message();
        message.setChatId(chatId);
        message.setSenderId(user.getId());
        message.setText(content);
        message.setType("text");
        message.setReplyToId(replyToId);
        message.setStatus(Status.SENT);

        messageService.create(message, user.getId());

        MessageResponse response = MessageResponse.from(message);
        messagingTemplate.convertAndSend("/topic/chat/" + chatId, response);

        return ResponseEntity.ok(response);
    }

    // ========================
    // ОТМЕТИТЬ ОДНО СООБЩЕНИЕ КАК ПРОЧИТАННОЕ
    // ========================
    @PostMapping("/{messageId}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable Long messageId,
            @AuthenticationPrincipal JwtEntity user
    ) {
        Message message = messageService.getById(messageId);
        ensureMember(message.getChatId(), user.getId());

        messageService.updateStatus(messageId, Status.READ);
        return ResponseEntity.ok().build();
    }

    // ========================
    // ОТМЕТИТЬ ВСЕ СООБЩЕНИЯ ЧАТА КАК ПРОЧИТАННЫЕ
    // ========================
    @PostMapping("/chat/{chatId}/read")
    public ResponseEntity<Void> markChatAsRead(
            @PathVariable Long chatId,
            @AuthenticationPrincipal JwtEntity user
    ) {
        ensureMember(chatId, user.getId());
        messageService.markChatAsRead(chatId, user.getId());
        return ResponseEntity.ok().build();
    }

    // ========================
    // СОЗДАТЬ ИЛИ НАЙТИ ПРИВАТНЫЙ ЧАТ
    // ========================
    @PostMapping("/private/{userId}")
    public ResponseEntity<Map<String, Long>> getOrCreatePrivateChat(
            @PathVariable Long userId,
            @AuthenticationPrincipal JwtEntity user
    ) {
        Optional<Chat> existing = chatService.findPrivateChat(user.getId(), userId);
        if (existing.isPresent()) {
            return ResponseEntity.ok(Map.of("chatId", existing.get().getId()));
        }

        Long chatId = chatService.createPrivateChat(user.getId());
        chatService.addMember(chatId, user.getId());
        if (!userId.equals(user.getId())) {
            chatService.addMember(chatId, userId);
        }
        return ResponseEntity.ok(Map.of("chatId", chatId));
    }

    // ========================
    // ХЕЛПЕРЫ
    // ========================

    private void ensureMember(Long chatId, Long userId) {
        if (!chatService.isMember(chatId, userId)) {
            throw new AccessDeniedException("Вы не являетесь участником чата");
        }
    }

    private Long parseLong(Object value) {
        if (value == null) return null;
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}