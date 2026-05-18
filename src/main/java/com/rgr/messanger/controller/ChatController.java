package com.rgr.messanger.controller;

import com.rgr.messanger.entity.chat.Chat;
import com.rgr.messanger.entity.user.User;
import com.rgr.messanger.service.ChatService;
import com.rgr.messanger.service.UserService;
import com.rgr.messanger.web.dto.chat.CreateGroupRequest;
import com.rgr.messanger.web.dto.chat.UpdateGroupRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;

    @GetMapping
    @ResponseBody
    public ResponseEntity<List<ChatResponse>> getMyChats(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        User me = userService.getByUsername(principal.getName());
        List<Chat> chats = chatService.findByUserId(me.getId());

        List<ChatResponse> response = chats.stream()
                .map(ChatResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    public record ChatResponse(
            Long   id,
            String type,
            String name,
            String avatarUrl,
            String lastMessage,
            String lastMessageType,
            boolean hasAttachment,
            String lastMessageTime,
            Long   interlocutorId,
            String interlocutorName,
            String interlocutorAvatar,
            int    unreadCount
    ) {
        public static ChatResponse from(Chat chat) {

            String lastMsgType = chat.getLastMessageType();
            boolean hasAttach  = lastMsgType != null &&
                    !lastMsgType.equals("text");

            return new ChatResponse(
                    chat.getId(),
                    chat.getType(),
                    chat.getName(),
                    chat.getAvatarUrl(),
                    chat.getLastMessage(),
                    lastMsgType,
                    hasAttach,
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

    @PostMapping("/group")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> createGroup(
            @Validated @RequestBody CreateGroupRequest request,
            Principal principal
    ) {
        if (principal == null) return ResponseEntity.status(401).build();
        User me = userService.getByUsername(principal.getName());
        Long chatId = chatService.createGroupChat(
                request.getName(), me.getId(), request.getMemberIds()
        );
        return ResponseEntity.ok(Map.of("chatId", chatId));
    }

    @PutMapping("/{chatId}")
    @ResponseBody
    public ResponseEntity<Void> updateGroup(
            @PathVariable Long chatId,
            @Validated @RequestBody UpdateGroupRequest request,
            Principal principal
    ) {
        if (principal == null) return ResponseEntity.status(401).build();
        User me = userService.getByUsername(principal.getName());
        chatService.updateChat(
                chatId, request.getName(), request.getAvatarUrl(), me.getId()
        );
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{chatId}")
    @ResponseBody
    public ResponseEntity<Void> deleteGroup(
            @PathVariable Long chatId,
            Principal principal
    ) {
        if (principal == null) return ResponseEntity.status(401).build();
        User me = userService.getByUsername(principal.getName());
        chatService.deleteChat(chatId, me.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{chatId}/members")
    @ResponseBody
    public ResponseEntity<Void> addMember(
            @PathVariable Long chatId,
            @RequestBody Map<String, Long> body,
            Principal principal
    ) {
        if (principal == null) return ResponseEntity.status(401).build();
        User me = userService.getByUsername(principal.getName());
        chatService.addMember(chatId, body.get("userId"), me.getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{chatId}/members/{userId}")
    @ResponseBody
    public ResponseEntity<Void> removeMember(
            @PathVariable Long chatId,
            @PathVariable Long userId,
            Principal principal
    ) {
        if (principal == null) return ResponseEntity.status(401).build();
        User me = userService.getByUsername(principal.getName());
        chatService.removeMember(chatId, userId, me.getId());
        return ResponseEntity.ok().build();
    }
}