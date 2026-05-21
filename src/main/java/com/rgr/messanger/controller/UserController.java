package com.rgr.messanger.controller;

import com.rgr.messanger.entity.user.User;
import com.rgr.messanger.service.FileStorageService;
import com.rgr.messanger.service.UserService;
import com.rgr.messanger.web.dto.user.UpdateProfileRequest;
import com.rgr.messanger.web.dto.user.UserResponse;
import com.rgr.messanger.web.dto.user.UserSearchResponse;
import com.rgr.messanger.web.security.JwtEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private static final long MAX_AVATAR_SIZE = 5L * 1024 * 1024; // 5 MB

    private final UserService        userService;
    private final FileStorageService fileStorageService;

    // ========================
    // ТЕКУЩИЙ ПОЛЬЗОВАТЕЛЬ
    // ========================
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal JwtEntity principal
    ) {
        User user = userService.getById(principal.getId());
        return ResponseEntity.ok(UserResponse.fromUser(user));
    }

    // ========================
    // ОБНОВИТЬ ПРОФИЛЬ
    // ========================
    @PatchMapping("/me")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal JwtEntity principal,
            @RequestBody UpdateProfileRequest request
    ) {
        User user = userService.getById(principal.getId());

        // Смена пароля — отдельный метод с проверкой старого
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            try {
                userService.updatePassword(
                        principal.getId(),
                        request.getOldPassword(),
                        request.getPassword()
                );
            } catch (IllegalStateException | IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", e.getMessage()));
            }
        }

        boolean changed = false;

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            user.setUsername(request.getUsername());
            changed = true;
        }

        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
            user.setAvatarUrl(request.getAvatarUrl());
            changed = true;
        }

        if (changed) {
            userService.update(user);
        }

        // Возвращаем актуального юзера
        User updated = userService.getById(principal.getId());
        return ResponseEntity.ok(UserResponse.fromUser(updated));
    }

    // ========================
    // ЗАГРУЗИТЬ АВАТАР
    // ========================
    @PostMapping("/me/avatar")
    public ResponseEntity<?> uploadAvatar(
            @AuthenticationPrincipal JwtEntity principal,
            @RequestParam("file") MultipartFile file
    ) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Только изображения"));
        }

        if (file.getSize() > MAX_AVATAR_SIZE) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Файл слишком большой (макс 5MB)"));
        }

        try {
            String avatarUrl = fileStorageService.saveAvatar(file);
            userService.updateAvatar(principal.getId(), avatarUrl);
            return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));

        } catch (Exception e) {
            log.error("Ошибка загрузки аватара: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Не удалось сохранить файл"));
        }
    }

    // ========================
    // HEARTBEAT (онлайн-статус)
    // ========================
    @PostMapping("/me/heartbeat")
    public ResponseEntity<Void> heartbeat(
            @AuthenticationPrincipal JwtEntity principal
    ) {
        userService.updateOnlineStatus(principal.getId(), true);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/me/offline")
    public ResponseEntity<Void> setOffline(
            @AuthenticationPrincipal JwtEntity principal
    ) {
        userService.updateOnlineStatus(principal.getId(), false);
        return ResponseEntity.ok().build();
    }

    // ========================
    // EMAIL УВЕДОМЛЕНИЯ
    // ========================
    @PatchMapping("/me/email-notifications")
    public ResponseEntity<Void> updateEmailNotifications(
            @AuthenticationPrincipal JwtEntity principal,
            @RequestBody Map<String, Boolean> body
    ) {
        Boolean value = body.get("emailNotifications");
        if (value == null) {
            return ResponseEntity.badRequest().build();
        }
        userService.updateEmailNotifications(principal.getId(), value);
        return ResponseEntity.ok().build();
    }

    // ========================
    // ПОЛУЧИТЬ ПО ID
    // ========================
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        User user = userService.getById(id);
        return ResponseEntity.ok(UserResponse.fromUser(user));
    }

    // ========================
    // ПОИСК
    // ========================
    @GetMapping("/search")
    public ResponseEntity<List<UserSearchResponse>> searchUsers(
            @RequestParam String username,
            @AuthenticationPrincipal JwtEntity principal
    ) {
        if (username == null || username.length() < 2) {
            return ResponseEntity.ok(List.of());
        }

        List<UserSearchResponse> response = userService.searchByUsername(username)
                .stream()
                .filter(u -> !u.getId().equals(principal.getId()))
                .map(UserSearchResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }
}