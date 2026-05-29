package com.rgr.messanger.web.mappers;

import com.rgr.messanger.entity.user.Role;
import com.rgr.messanger.entity.user.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

//превратить строку результата SQL-запроса в объект
public final class UserRowMapper {

    private UserRowMapper() {
    }

    /**
     * ResultSetExtractor — для запросов с LEFT JOIN user_roles.
     * Агрегирует все роли одного юзера в Set.
     */
    public static User mapRow(ResultSet rs) throws SQLException {
        User user = null;
        Set<Role> roles = new HashSet<>();

        while (rs.next()) {
            long currentId = rs.getLong("user_id");

            if (user == null) {
                user = mapUserFields(rs);
            } else if (!user.getId().equals(currentId)) {
                // SQL вернул нескольких пользователей — берём только первого
                break;
            }

            String role = rs.getString("user_role");
            if (role != null && !role.isBlank()) {
                try {
                    roles.add(Role.valueOf(role));
                } catch (IllegalArgumentException ignored) {
                    // неизвестная роль в БД — пропускаем
                }
            }
        }

        if (user != null) {
            user.setRoles(roles);
        }

        return user;
    }

    /**
     * Базовый маппинг одной строки.
     * Используется в списках / поиске.
     */
    public static User mapBasicRow(ResultSet rs) throws SQLException {
        User user = mapUserFields(rs);

        String role = null;
        try {
            role = rs.getString("user_role");
        } catch (SQLException ignored) {
            // колонка может отсутствовать в некоторых select'ах
        }

        if (role != null && !role.isBlank()) {
            Set<Role> roles = new HashSet<>();
            try {
                roles.add(Role.valueOf(role));
            } catch (IllegalArgumentException ignored) {
                // ignore bad db value
            }
            user.setRoles(roles);
        }

        return user;
    }

    private static User mapUserFields(ResultSet rs) throws SQLException {
        // Создаем пустой объект User, в который будем переносить данные из ResultSet
        User user = new User();

        // Читаем из результата SQL-запроса основные поля пользователя
        // Здесь используются алиасы колонок вида user_id, user_username и т.д.
        user.setId(rs.getLong("user_id"));
        user.setUsername(rs.getString("user_username"));
        user.setEmail(rs.getString("user_email"));

        // Признак подтверждения email
        user.setEmailVerified(rs.getBoolean("user_email_verified"));

        // Настройка email-уведомлений
        user.setEmailNotifications(rs.getBoolean("user_email_notifications"));

        // Признак блокировки пользователя
        user.setBlocked(rs.getBoolean("user_blocked"));

        // Хеш пароля пользователя
        user.setPassword(rs.getString("user_password"));

        // Ссылка на аватар пользователя
        user.setAvatarUrl(rs.getString("user_avatar_url"));

        // Текущий статус пользователя, например online / offline
        user.setStatus(rs.getString("user_status"));

        // Поле last_seen в БД имеет тип Timestamp,
        // поэтому сначала читаем его как Timestamp
        Timestamp lastSeen = rs.getTimestamp("user_last_seen");

        // Если значение не null, преобразуем Timestamp в LocalDateTime
        // и записываем в объект User
        if (lastSeen != null) {
            user.setLastSeen(lastSeen.toLocalDateTime());
        }

        // Дата создания пользователя
        Timestamp createdAt = rs.getTimestamp("user_created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }

        // Дата последнего обновления пользователя
        Timestamp updatedAt = rs.getTimestamp("user_updated_at");
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        // Возвращаем полностью заполненный объект User
        return user;
    }
}