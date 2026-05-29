package com.rgr.messanger.web.dto.user;

import com.rgr.messanger.entity.user.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

//расширенный DTO для отдачи на фронтенд
public record UserResponse(
        Long    id,
        String  username,
        String  email,
        String  avatarUrl,
        String  status,
        boolean emailNotifications,
        String  lastSeen
) {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

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
        if ("online".equals(status)) return "в сети";
        if (lastSeen == null)        return "не в сети";

        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(lastSeen, now);
        long hours   = ChronoUnit.HOURS.between(lastSeen, now);
        long days    = ChronoUnit.DAYS.between(lastSeen, now);

        if (minutes < 1)  return "был(а) только что";
        if (minutes < 60) return "был(а) " + minutes + " мин. назад";
        if (hours < 24)   return "был(а) " + hours + " ч. назад";
        if (days == 1)    return "был(а) вчера";

        return "был(а) " + lastSeen.format(DATE_FORMAT);
    }
}