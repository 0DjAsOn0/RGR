package com.rgr.messanger.repository.impl;

import com.rgr.messanger.entity.user.Role;
import com.rgr.messanger.entity.user.User;
import com.rgr.messanger.repository.UserRepo;
import com.rgr.messanger.web.mappers.UserRowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepoImpl implements UserRepo {

    private static final String STATUS_ONLINE = "online";
    private static final String STATUS_OFFLINE = "offline";

    private static final ResultSetExtractor<User> USER_EXTRACTOR =
            UserRowMapper::mapRow;

    private final JdbcTemplate jdbcTemplate;

    // ========================
    // SELECT-БАЗА
    // ========================
    private static final String SELECT_USER_BASE = """
        SELECT u.id                  AS user_id,
               u.username            AS user_username,
               u.email               AS user_email,
               u.password            AS user_password,
               u.avatar_url          AS user_avatar_url,
               u.status              AS user_status,
               u.last_seen           AS user_last_seen,
               u.created_at          AS user_created_at,
               u.updated_at          AS user_updated_at,
               u.email_verified      AS user_email_verified,
               u.email_notifications AS user_email_notifications,
               u.blocked             AS user_blocked,
               ur.role               AS user_role
        FROM users u
        LEFT JOIN user_roles ur ON ur.user_id = u.id
        """;

    private static final String FIND_BY_ID = SELECT_USER_BASE + " WHERE u.id = ?";
    private static final String FIND_BY_USERNAME = SELECT_USER_BASE + " WHERE u.username = ?";
    private static final String FIND_BY_EMAIL = SELECT_USER_BASE + " WHERE LOWER(u.email) = LOWER(?)";
    private static final String SEARCH_BY_USERNAME = SELECT_USER_BASE + " WHERE u.username ILIKE ? ORDER BY u.username LIMIT 20";
    private static final String FIND_ALL = SELECT_USER_BASE + " ORDER BY u.id DESC";

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(
                jdbcTemplate.query(FIND_BY_ID, USER_EXTRACTOR, id)
        );
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(
                jdbcTemplate.query(FIND_BY_USERNAME, USER_EXTRACTOR, username)
        );
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return Optional.ofNullable(
                jdbcTemplate.query(FIND_BY_EMAIL, USER_EXTRACTOR, email)
        );
    }

    @Override
    public List<User> searchByUsername(String username) {
        return jdbcTemplate.query(
                SEARCH_BY_USERNAME,
                (rs, rowNum) -> UserRowMapper.mapBasicRow(rs),
                "%" + username + "%"
        );
    }

    @Override
    public List<User> findAll() {
        return jdbcTemplate.query(
                FIND_ALL,
                rs -> {
                    List<User> users = new java.util.ArrayList<>();
                    User currentUser = null;
                    java.util.Set<Role> currentRoles = new java.util.HashSet<>();
                    Long currentId = null;

                    while (rs.next()) {
                        Long rowUserId = rs.getLong("user_id");

                        if (currentId == null || !currentId.equals(rowUserId)) {
                            if (currentUser != null) {
                                currentUser.setRoles(new java.util.HashSet<>(currentRoles));
                                users.add(currentUser);
                            }

                            currentUser = UserRowMapper.mapBasicRow(rs);
                            currentRoles = new java.util.HashSet<>();
                            currentId = rowUserId;
                        }

                        String role = rs.getString("user_role");
                        if (role != null && !role.isBlank()) {
                            try {
                                currentRoles.add(Role.valueOf(role));
                            } catch (IllegalArgumentException ignored) {
                                // ignore invalid role
                            }
                        }
                    }

                    if (currentUser != null) {
                        currentUser.setRoles(new java.util.HashSet<>(currentRoles));
                        users.add(currentUser);
                    }

                    return users;
                }
        );
    }

    // ========================
    // СОЗДАНИЕ
    // ========================
    private static final String CREATE = """
        INSERT INTO users (
            username,
            email,
            password,
            avatar_url,
            status,
            last_seen,
            email_verified,
            email_notifications,
            blocked
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

    @Override
    public void create(User user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(
                    CREATE, new String[]{"id"}
            );
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPassword());
            stmt.setString(4, user.getAvatarUrl());
            stmt.setString(5, user.getStatus() != null ? user.getStatus() : STATUS_OFFLINE);
            stmt.setTimestamp(6, user.getLastSeen() != null
                    ? Timestamp.valueOf(user.getLastSeen())
                    : Timestamp.valueOf(LocalDateTime.now()));
            stmt.setBoolean(7, user.isEmailVerified());
            stmt.setBoolean(8, user.isEmailNotifications());
            stmt.setBoolean(9, user.isBlocked());
            return stmt;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to retrieve generated user id");
        }
        user.setId(key.longValue());
    }

    // ========================
    // РОЛИ
    // ========================
    private static final String INSERT_USER_ROLE = """
        INSERT INTO user_roles (user_id, role)
        VALUES (?, ?)
        ON CONFLICT DO NOTHING
        """;

    private static final String DELETE_USER_ROLES = """
        DELETE FROM user_roles
        WHERE user_id = ?
        """;

    @Override
    public void insertUserRole(Long userId, Role role) {
        jdbcTemplate.update(INSERT_USER_ROLE, userId, role.name());
    }

    @Override
    public void replaceUserRoles(Long userId, List<Role> roles) {
        jdbcTemplate.update(DELETE_USER_ROLES, userId);
        if (roles == null || roles.isEmpty()) {
            return;
        }
        for (Role role : roles) {
            jdbcTemplate.update(INSERT_USER_ROLE, userId, role.name());
        }
    }

    // ========================
    // ОБНОВЛЕНИЕ ОТДЕЛЬНЫХ ПОЛЕЙ
    // ========================

    private static final String UPDATE_USERNAME = """
        UPDATE users
        SET username = ?, updated_at = NOW()
        WHERE id = ?
        """;

    @Override
    public void updateUsername(Long id, String username) {
        jdbcTemplate.update(UPDATE_USERNAME, username, id);
    }

    private static final String UPDATE_AVATAR = """
        UPDATE users
        SET avatar_url = ?, updated_at = NOW()
        WHERE id = ?
        """;

    @Override
    public void updateAvatar(Long id, String avatarUrl) {
        jdbcTemplate.update(UPDATE_AVATAR, avatarUrl, id);
    }

    private static final String UPDATE_PASSWORD = """
        UPDATE users
        SET password = ?, updated_at = NOW()
        WHERE id = ?
        """;

    @Override
    public void updatePassword(Long userId, String encodedPassword) {
        jdbcTemplate.update(UPDATE_PASSWORD, encodedPassword, userId);
    }

    private static final String UPDATE_STATUS = """
        UPDATE users
        SET status     = ?,
            last_seen  = NOW(),
            updated_at = NOW()
        WHERE id = ?
        """;

    @Override
    public void updateOnlineStatus(Long userId, boolean online) {
        jdbcTemplate.update(
                UPDATE_STATUS,
                online ? STATUS_ONLINE : STATUS_OFFLINE,
                userId
        );
    }

    private static final String VERIFY_EMAIL = """
        UPDATE users
        SET email_verified = TRUE,
            updated_at     = NOW()
        WHERE id = ?
        """;

    @Override
    public void verifyEmail(Long userId) {
        jdbcTemplate.update(VERIFY_EMAIL, userId);
    }

    private static final String UPDATE_EMAIL_NOTIFICATIONS = """
        UPDATE users
        SET email_notifications = ?,
            updated_at          = NOW()
        WHERE id = ?
        """;

    @Override
    public void updateEmailNotifications(Long userId, boolean emailNotifications) {
        jdbcTemplate.update(UPDATE_EMAIL_NOTIFICATIONS, emailNotifications, userId);
    }

    private static final String UPDATE_BLOCKED = """
        UPDATE users
        SET blocked    = ?,
            updated_at = NOW()
        WHERE id = ?
        """;

    @Override
    public void updateBlocked(Long userId, boolean blocked) {
        jdbcTemplate.update(UPDATE_BLOCKED, blocked, userId);
    }

    // ========================
    // ПОЛНОЕ ОБНОВЛЕНИЕ
    // ========================
    private static final String UPDATE = """
        UPDATE users
        SET username            = ?,
            email               = ?,
            password            = ?,
            avatar_url          = COALESCE(?, avatar_url),
            status              = COALESCE(?, status),
            last_seen           = COALESCE(?, last_seen),
            email_verified      = ?,
            email_notifications = ?,
            blocked             = ?,
            updated_at          = NOW()
        WHERE id = ?
        """;

    @Override
    public void update(User user) {
        jdbcTemplate.update(
                UPDATE,
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getAvatarUrl(),
                user.getStatus(),
                user.getLastSeen() != null
                        ? Timestamp.valueOf(user.getLastSeen())
                        : null,
                user.isEmailVerified(),
                user.isEmailNotifications(),
                user.isBlocked(),
                user.getId()
        );
    }

    // ========================
    // ПРОВЕРКА ВЛАДЕЛЬЦА СООБЩЕНИЯ
    // ========================
    private static final String IS_MESSAGE_OWNER = """
        SELECT COUNT(1)
        FROM messages
        WHERE id = ? AND sender_id = ?
        """;

    @Override
    public boolean isMessageOwner(Long userId, Long messageId) {
        Integer count = jdbcTemplate.queryForObject(
                IS_MESSAGE_OWNER,
                Integer.class,
                messageId,
                userId
        );
        return count != null && count > 0;
    }

    // ========================
    // СТАТИСТИКА
    // ========================
    private static final String COUNT_USERS = """
        SELECT COUNT(*) FROM users
        """;

    @Override
    public long countUsers() {
        Long count = jdbcTemplate.queryForObject(COUNT_USERS, Long.class);
        return count != null ? count : 0L;
    }

    // ========================
    // УДАЛИТЬ
    // ========================
    private static final String DELETE = """
        DELETE FROM users
        WHERE id = ?
        """;

    @Override
    public void delete(Long id) {
        jdbcTemplate.update(DELETE, id);
    }
}