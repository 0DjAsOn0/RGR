package com.rgr.messanger.web.mappers;

import com.rgr.messanger.entity.user.Role;
import com.rgr.messanger.entity.user.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

public final class UserRowMapper {

    private UserRowMapper() {}

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
            } else if (user.getId() != currentId) {
                // защита: SQL вернул нескольких юзеров — обрабатываем только первого
                break;
            }

            String role = rs.getString("user_role");
            if (role != null) {
                try {
                    roles.add(Role.valueOf(role));
                } catch (IllegalArgumentException e) {
                    // неизвестная роль в БД — игнорируем
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
     * Используется для поиска (без ролей).
     */
    public static User mapBasicRow(ResultSet rs) throws SQLException {
        return mapUserFields(rs);
    }

    private static User mapUserFields(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("user_id"));
        user.setUsername(rs.getString("user_username"));
        user.setEmail(rs.getString("user_email"));
        user.setEmailVerified(rs.getBoolean("user_email_verified"));
        user.setEmailNotifications(rs.getBoolean("user_email_notifications"));
        user.setPassword(rs.getString("user_password"));
        user.setAvatarUrl(rs.getString("user_avatar_url"));
        user.setStatus(rs.getString("user_status"));

        Timestamp lastSeen = rs.getTimestamp("user_last_seen");
        if (lastSeen != null) user.setLastSeen(lastSeen.toLocalDateTime());

        Timestamp createdAt = rs.getTimestamp("user_created_at");
        if (createdAt != null) user.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp updatedAt = rs.getTimestamp("user_updated_at");
        if (updatedAt != null) user.setUpdatedAt(updatedAt.toLocalDateTime());

        return user;
    }
}