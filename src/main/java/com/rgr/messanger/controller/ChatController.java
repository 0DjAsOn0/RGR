package com.rgr.messanger.controller;

import com.rgr.messanger.entity.chat.Chat;
import com.rgr.messanger.service.ChatService;
import com.rgr.messanger.web.dto.chat.AddMemberRequest;
import com.rgr.messanger.web.dto.chat.ChatDto;
import com.rgr.messanger.web.dto.chat.ChatInfoResponse;
import com.rgr.messanger.web.dto.chat.ChatMemberResponse;
import com.rgr.messanger.web.dto.chat.ChatResponse;
import com.rgr.messanger.web.dto.chat.ChatUpdateRequest;
import com.rgr.messanger.web.dto.chat.CreateGroupRequest;
import com.rgr.messanger.web.dto.chat.UpdateGroupRequest;
import com.rgr.messanger.web.security.JwtEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // ========================
    // СПИСОК МОИХ ЧАТОВ
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
    // ИНФО О ЧАТЕ
    // ========================

    @GetMapping("/{chatId}")
    public ChatInfoResponse getChatInfo(
            @PathVariable Long chatId,
            @AuthenticationPrincipal JwtEntity user
    ) {
        return chatService.getChatInfo(chatId, user.getId());
    }

    @GetMapping("/{chatId}/members")
    public List<ChatMemberResponse> getChatMembers(
            @PathVariable Long chatId,
            @AuthenticationPrincipal JwtEntity user
    ) {
        return chatService.getChatMembers(chatId, user.getId());
    }

    // ========================
    // СОЗДАТЬ ГРУППУ
    // ========================

    @PostMapping("/group")
    public ResponseEntity<Map<String, Long>> createGroup(
            @Validated @RequestBody CreateGroupRequest request,
            @AuthenticationPrincipal JwtEntity user
    ) {
        Boolean isPublic = request.getIsPublic() != null ? request.getIsPublic() : false;

        Long chatId = chatService.createGroupChat(
                request.getName(),
                user.getId(),
                request.getMemberIds(),
                isPublic
        );
        return ResponseEntity.ok(Map.of("chatId", chatId));
    }

    // ========================
    // ОБНОВЛЕНИЕ ГРУППЫ
    // ========================

    @PutMapping("/{chatId}")
    public ResponseEntity<Void> updateGroup(
            @PathVariable Long chatId,
            @Validated @RequestBody UpdateGroupRequest request,
            @AuthenticationPrincipal JwtEntity user
    ) {
        chatService.updateChat(
                chatId,
                request.getName(),
                request.getAvatarUrl(),
                user.getId()
        );
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{chatId}")
    public ResponseEntity<Void> updateChatName(
            @PathVariable Long chatId,
            @RequestBody ChatUpdateRequest request,
            @AuthenticationPrincipal JwtEntity user
    ) {
        chatService.updateChatName(chatId, request.getName(), user.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{chatId}/avatar")
    public Map<String, String> updateChatAvatar(
            @PathVariable Long chatId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal JwtEntity user
    ) {
        String url = chatService.updateChatAvatar(chatId, file, user.getId());
        return Map.of("avatarUrl", url);
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable Long chatId,
            @AuthenticationPrincipal JwtEntity user
    ) {
        chatService.deleteChat(chatId, user.getId());
        return ResponseEntity.ok().build();
    }

    // ========================
    // УЧАСТНИКИ
    // ========================

    @PostMapping("/{chatId}/members")
    public ResponseEntity<Void> addMember(
            @PathVariable Long chatId,
            @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal JwtEntity user
    ) {
        chatService.addMember(chatId, request.getUserId(), user.getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{chatId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long chatId,
            @PathVariable Long userId,
            @AuthenticationPrincipal JwtEntity user
    ) {
        chatService.removeMember(chatId, userId, user.getId());
        return ResponseEntity.ok().build();
    }

    // ========================
    // ПОИСК ПУБЛИЧНЫХ ГРУПП
    // ========================

    @GetMapping("/search")
    public ResponseEntity<List<ChatResponse>> searchGroups(@RequestParam String query) {
        List<ChatDto> foundChats = chatService.searchConversations(query);

        List<ChatResponse> response = foundChats.stream()
                .map(ChatResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{chatId}/privacy")
    public ResponseEntity<Void> updateGroupPrivacy(
            @PathVariable Long chatId,
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal JwtEntity user
    ) {
        chatService.updateChatPrivacy(chatId, body.get("isPublic"), user.getId());
        return ResponseEntity.ok().build();
    }

    // ========================
    // ВСТУПИТЬ В ПУБЛИЧНУЮ ГРУППУ
    // ========================
    @PostMapping("/{chatId}/join")
    public ResponseEntity<Void> joinPublicGroup(
            @PathVariable Long chatId,
            @AuthenticationPrincipal JwtEntity user
    ) {
        chatService.joinPublicGroup(chatId, user.getId());
        return ResponseEntity.ok().build();
    }
}