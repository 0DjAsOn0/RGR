package com.rgr.messanger.controller;

import com.rgr.messanger.entity.user.User;
import com.rgr.messanger.service.UserService;
import com.rgr.messanger.service.impl.EmailServiceImpl;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
public class MainController {

    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final EmailServiceImpl emailServiceImpl;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public MainController(UserService userService,
                          ObjectMapper objectMapper,
                          EmailServiceImpl emailServiceImpl,
                          PasswordEncoder passwordEncoder) {
        this.userService      = userService;
        this.objectMapper     = objectMapper;
        this.emailServiceImpl = emailServiceImpl;
        this.passwordEncoder  = passwordEncoder;
    }


    @GetMapping("/api/v1/users/me")
    @ResponseBody
    public ResponseEntity<UserResponse> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userService.getByUsername(principal.getName());

        return ResponseEntity.ok(UserResponse.fromUser(user));
    }

    @PatchMapping("/api/v1/users/me")
    @ResponseBody
    public ResponseEntity<?> updateProfile(
            Principal principal,
            @RequestBody Map<String, String> body
    ) {
        if (principal == null) return ResponseEntity.status(401).build();

        try {
            User user = userService.getByUsername(principal.getName());

            String newUsername  = body.get("username");
            String oldPassword  = body.get("oldPassword");
            String newPassword  = body.get("password");
            String newAvatar    = body.get("avatarUrl");


            if (newPassword != null && !newPassword.isBlank()) {

                if (oldPassword == null || oldPassword.isBlank()) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Введите текущий пароль"));
                }


                if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Неверный текущий пароль"));
                }

                user.setPassword(newPassword);
                user.setPasswordConfirmation(newPassword);
            }

            if (newUsername != null && !newUsername.isBlank()) {
                user.setUsername(newUsername);
            }

            if (newAvatar != null && !newAvatar.isBlank()) {
                user.setAvatarUrl(newAvatar);
            }

            userService.update(user);

            return ResponseEntity.ok(UserResponse.fromUser(user));

        } catch (Exception e) {
            log.error("Ошибка обновления профиля: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/api/v1/users/me/avatar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> uploadAvatar(
            Principal principal,
            @RequestParam("file") MultipartFile file
    ) {
        if (principal == null) return ResponseEntity.status(401).build();

        try {
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Только изображения"));
            }

            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Файл слишком большой (макс 5MB)"));
            }

            String ext      = getExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID() + "." + ext;

            Path uploadDir = Paths.get("src/main/resources/static/avatars");
            Files.createDirectories(uploadDir);

            Path filePath = uploadDir.resolve(fileName);
            Files.copy(file.getInputStream(), filePath,
                    StandardCopyOption.REPLACE_EXISTING);

            String avatarUrl = "/avatars/" + fileName;

            User user = userService.getByUsername(principal.getName());

            userService.updateAvatar(user.getId(), avatarUrl);

            return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));

        } catch (Exception e) {
            log.error("Ошибка загрузки аватара: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    @PostMapping("/api/v1/users/me/heartbeat")
    @ResponseBody
    public ResponseEntity<Void> heartbeat(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        User user = userService.getByUsername(principal.getName());
        userService.updateOnlineStatus(user.getId(), true);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/v1/users/me/offline")
    @ResponseBody
    public ResponseEntity<Void> setOffline(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        User user = userService.getByUsername(principal.getName());
        userService.updateOnlineStatus(user.getId(), false);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/api/v1/users/me/email-notifications")
    @ResponseBody
    public ResponseEntity<Void> updateEmailNotifications(
            Principal principal,
            @RequestBody Map<String, Boolean> body
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userService.getByUsername(principal.getName());
        boolean value = body.get("emailNotifications");
        userService.updateEmailNotifications(user.getId(), value);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/v1/users/search")
    @ResponseBody
    public ResponseEntity<List<UserSearchResponse>> searchUsers(
            @RequestParam String username,
            Principal principal
    ) {
        if (principal == null) return ResponseEntity.status(401).build();

        // Не ищем если меньше 2 символов
        if (username == null || username.length() < 2) {
            return ResponseEntity.ok(List.of());
        }

        User me = userService.getByUsername(principal.getName());
        List<User> users = userService.searchByUsername(username);

        List<UserSearchResponse> response = users.stream()
                // Исключаем себя из результатов
                .filter(u -> !u.getId().equals(me.getId()))
                .map(UserSearchResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    // DTO для поиска
    public record UserSearchResponse(
            Long id,
            String username,
            String avatarUrl,
            String status
    ) {
        public static UserSearchResponse from(User user) {
            return new UserSearchResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getAvatarUrl(),
                    user.getStatus()
            );
        }
    }

    public record UserResponse(
            Long id,
            String username,
            String email,
            String avatarUrl,
            String status,
            boolean emailNotifications,
            String lastSeen
    ) {
        public static UserResponse fromUser(User user) {
            return new UserResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getAvatarUrl(),
                    user.getStatus(),
                    user.isEmailNotifications(),
                    formatLastSeen(user.getLastSeen(), user.getStatus())
            );
        }

        private static String formatLastSeen(LocalDateTime lastSeen, String status) {

            if ("online".equals(status)) {
                return "в сети";
            }
            if (lastSeen == null) {
                return "не в сети";
            }

            LocalDateTime now = LocalDateTime.now();
            long minutes = ChronoUnit.MINUTES.between(lastSeen, now);
            long hours = ChronoUnit.HOURS.between(lastSeen, now);
            long days = ChronoUnit.DAYS.between(lastSeen, now);

            if (minutes < 1) return "был(а) только что";
            if (minutes < 60) return "был(а) " + minutes + " мин. назад";
            if (hours < 24) return "был(а) " + hours + " ч. назад";
            if (days == 1) return "был(а) вчера";

            return "был(а) " + lastSeen.format(
                    DateTimeFormatter.ofPattern("dd.MM.yyyy")
            );
        }
    }
}
