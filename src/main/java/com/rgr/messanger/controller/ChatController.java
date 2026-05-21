package com.rgr.messanger.controller;

import com.rgr.messanger.service.ChatService;
import com.rgr.messanger.web.dto.chat.ChatDto;
import com.rgr.messanger.web.dto.chat.ChatResponse;
import com.rgr.messanger.web.dto.chat.CreateGroupRequest;
import com.rgr.messanger.web.dto.chat.UpdateGroupRequest;
import com.rgr.messanger.web.security.JwtEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // ========================
    // ПОЛУЧИТЬ ВСЕ МОИ ЧАТЫ
    // ========================
    @GetMapping
    public ResponseEntity<List<ChatResponse>> getMyChats(
            @AuthenticationPrincipal JwtEntity user
    ) {
        List<ChatDto> chats = chatService.findByUserId(user.getId());

        List<ChatResponse> response = chats.stream()
                .map(ChatResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    // ========================
    // СОЗДАТЬ ГРУППУ
    // ========================
    @PostMapping("/group")
    public ResponseEntity<Map<String, Long>> createGroup(
            @Validated @RequestBody CreateGroupRequest request,
            @AuthenticationPrincipal JwtEntity user
    ) {
        Long chatId = chatService.createGroupChat(
                request.getName(), user.getId(), request.getMemberIds()
        );
        return ResponseEntity.ok(Map.of("chatId", chatId));
    }

    // ========================
    // ОБНОВИТЬ ГРУППУ
    // ========================
    @PutMapping("/{chatId}")
    public ResponseEntity<Void> updateGroup(
            @PathVariable Long chatId,
            @Validated @RequestBody UpdateGroupRequest request,
            @AuthenticationPrincipal JwtEntity user
    ) {
        chatService.updateChat(
                chatId, request.getName(), request.getAvatarUrl(), user.getId()
        );
        return ResponseEntity.ok().build();
    }

    // ========================
    // УДАЛИТЬ ГРУППУ
    // ========================
    @DeleteMapping("/{chatId}")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable Long chatId,
            @AuthenticationPrincipal JwtEntity user
    ) {
        chatService.deleteChat(chatId, user.getId());
        return ResponseEntity.ok().build();
    }

    // ========================
    // ДОБАВИТЬ УЧАСТНИКА
    // ========================
    @PostMapping("/{chatId}/members")
    public ResponseEntity<Void> addMember(
            @PathVariable Long chatId,
            @RequestBody Map<String, Long> body,
            @AuthenticationPrincipal JwtEntity user
    ) {
        Long memberId = body.get("userId");
        if (memberId == null) {
            return ResponseEntity.badRequest().build();
        }
        chatService.addMember(chatId, memberId, user.getId());
        return ResponseEntity.ok().build();
    }

    // ========================
    // УДАЛИТЬ УЧАСТНИКА
    // ========================
    @DeleteMapping("/{chatId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long chatId,
            @PathVariable Long userId,
            @AuthenticationPrincipal JwtEntity user
    ) {
        chatService.removeMember(chatId, userId, user.getId());
        return ResponseEntity.ok().build();
    }
}