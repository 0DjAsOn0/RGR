package com.rgr.messanger.controller;

import com.rgr.messanger.entity.chat.Chat;
import com.rgr.messanger.entity.user.User;
import com.rgr.messanger.repository.ChatRepo;
import com.rgr.messanger.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatRepo chatRepo;
    private final UserService userService;

    // Получить список чатов текущего пользователя
    @GetMapping
    @ResponseBody
    public ResponseEntity<List<ChatResponse>> getMyChats(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        User me = userService.getByUsername(principal.getName());
        List<Chat> chats = chatRepo.findByUserId(me.getId());

        List<ChatResponse> response = chats.stream()
                .map(ChatResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    // DTO
    public record ChatResponse(
            Long id,
            String type,
            String name,
            String avatarUrl,
            String lastMessage,
            String lastMessageTime,
            Long interlocutorId,
            String interlocutorName,
            String interlocutorAvatar,
            int unreadCount
    ) {
        public static ChatResponse from(Chat chat) {
            return new ChatResponse(
                    chat.getId(),
                    chat.getType(),
                    chat.getName(),
                    chat.getAvatarUrl(),
                    chat.getLastMessage(),
                    chat.getLastMessageTime() != null
                            ? chat.getLastMessageTime()
                              .format(DateTimeFormatter.ofPattern("HH:mm"))
                            : "",
                    chat.getInterlocutorId(),
                    chat.getInterlocutorName(),
                    chat.getInterlocutorAvatar(),
                    chat.getUnreadCount()
            );
        }
    }
}
