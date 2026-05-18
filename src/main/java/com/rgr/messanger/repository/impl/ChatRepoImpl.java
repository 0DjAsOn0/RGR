package com.rgr.messanger.repository.impl;

import com.rgr.messanger.entity.chat.Chat;
import com.rgr.messanger.repository.ChatRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChatRepoImpl implements ChatRepo {

    private final JdbcTemplate jdbcTemplate;

    // ========================
    // ВСЕ ЧАТЫ ПОЛЬЗОВАТЕЛЯ
    // ========================
    private static final String FIND_BY_USER_ID = """
        SELECT
            c.id               as chat_id,
            c.type             as chat_type,
            c.name             as chat_name,
            c.avatar_url       as chat_avatar_url,
            c.updated_at       as chat_updated_at,
            m.text             as last_message_text,
            m.type             as last_message_type,
            m.send_date        as last_message_time,
            CASE WHEN c.type = 'private' THEN u.id         ELSE NULL END as interlocutor_id,
            CASE WHEN c.type = 'private' THEN u.username   ELSE NULL END as interlocutor_name,
            CASE WHEN c.type = 'private' THEN u.avatar_url ELSE NULL END as interlocutor_avatar,
            COUNT(unread.id)   as unread_count
        FROM chats c
        JOIN chat_members cm ON cm.chat_id = c.id AND cm.user_id = ?
        LEFT JOIN chat_members cm2 ON cm2.chat_id = c.id
            AND cm2.user_id != ?
            AND c.type = 'private'
        LEFT JOIN users u ON u.id = cm2.user_id
        LEFT JOIN messages m ON m.id = (
            SELECT id FROM messages
            WHERE chat_id = c.id AND is_deleted = FALSE
            ORDER BY send_date DESC
            LIMIT 1
        )
        LEFT JOIN messages unread ON unread.chat_id = c.id
            AND unread.sender_id != ?
            AND unread.is_deleted = FALSE
            AND unread.id > COALESCE((
                SELECT last_read_msg FROM message_reads
                WHERE chat_id = c.id AND user_id = ?
            ), 0)
        GROUP BY c.id, c.type, c.name, c.avatar_url, c.updated_at,
                 m.text, m.type, m.send_date,
                 u.id, u.username, u.avatar_url
        ORDER BY
            CASE WHEN c.name = 'Заметки' THEN 0 ELSE 1 END,
            COALESCE(m.send_date, c.created_at) DESC
        """;

    @Override
    public List<Chat> findByUserId(Long userId) {
        return jdbcTemplate.query(
                FIND_BY_USER_ID,
                (rs, rowNum) -> {
                    Chat chat = new Chat();
                    chat.setId(rs.getLong("chat_id"));
                    chat.setType(rs.getString("chat_type"));
                    chat.setName(rs.getString("chat_name"));
                    chat.setAvatarUrl(rs.getString("chat_avatar_url"));
                    chat.setLastMessage(rs.getString("last_message_text"));
                    chat.setLastMessageType(rs.getString("last_message_type"));
                    chat.setInterlocutorId(rs.getLong("interlocutor_id"));
                    chat.setInterlocutorName(rs.getString("interlocutor_name"));
                    chat.setInterlocutorAvatar(rs.getString("interlocutor_avatar"));
                    chat.setUnreadCount(rs.getInt("unread_count"));

                    Timestamp lastTime = rs.getTimestamp("last_message_time");
                    if (lastTime != null) {
                        chat.setLastMessageTime(lastTime.toLocalDateTime());
                    }
                    return chat;
                },
                userId, userId, userId, userId
        );
    }

    // ========================
    // ЗАМЕТКИ
    // ========================
    private static final String CREATE_NOTES_CHAT = """
            WITH new_chat AS (
                INSERT INTO chats (type, name, creator_id)
                VALUES ('private', 'Заметки', ?)
                RETURNING id
            )
            INSERT INTO chat_members (chat_id, user_id, role)
            SELECT id, ?, 'owner'
            FROM new_chat
            """;

    @Override
    public void createNotesChat(Long userId) {
        jdbcTemplate.update(CREATE_NOTES_CHAT, userId, userId);
    }

    // ========================
    // НАЙТИ ПРИВАТНЫЙ ЧАТ
    // ========================
    private static final String FIND_PRIVATE_CHAT = """
            SELECT c.id as chat_id
            FROM chats c
            JOIN chat_members cm1 ON cm1.chat_id = c.id AND cm1.user_id = ?
            JOIN chat_members cm2 ON cm2.chat_id = c.id AND cm2.user_id = ?
            WHERE c.type = 'private'
              AND cm1.user_id != cm2.user_id
            LIMIT 1
            """;

    @Override
    public Optional<Chat> findPrivateChat(Long userId1, Long userId2) {
        List<Chat> result = jdbcTemplate.query(
                FIND_PRIVATE_CHAT,
                (rs, rowNum) -> {
                    Chat chat = new Chat();
                    chat.setId(rs.getLong("chat_id"));
                    return chat;
                },
                userId1, userId2
        );
        return result.stream().findFirst();
    }

    // ========================
    // ПОЛУЧИТЬ ID УЧАСТНИКОВ
    // ========================
    private static final String GET_CHAT_MEMBER_IDS = """
        SELECT user_id FROM chat_members
        WHERE chat_id = ?
        """;

    @Override
    public List<Long> getChatMemberIds(Long chatId) {
        return jdbcTemplate.query(
                GET_CHAT_MEMBER_IDS,
                (rs, rowNum) -> rs.getLong("user_id"),
                chatId
        );
    }

    // ========================
    // СОЗДАТЬ ПРИВАТНЫЙ ЧАТ
    // ========================
    private static final String CREATE_CHAT = """
            INSERT INTO chats (type, creator_id)
            VALUES ('private', ?)
            """;

    @Override
    public Long createPrivateChat(Long creatorId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(
                    CREATE_CHAT, PreparedStatement.RETURN_GENERATED_KEYS
            );
            stmt.setLong(1, creatorId);
            return stmt;
        }, keyHolder);

        return ((Number) keyHolder.getKeys().get("id")).longValue();
    }

    // ========================
    // ДОБАВИТЬ УЧАСТНИКА
    // ========================
    private static final String ADD_MEMBER = """
            INSERT INTO chat_members (chat_id, user_id)
            VALUES (?, ?)
            ON CONFLICT DO NOTHING
            """;

    @Override
    public void addMember(Long chatId, Long userId) {
        jdbcTemplate.update(ADD_MEMBER, chatId, userId);
    }

    // ========================
    // НАЙТИ ЧАТ ПО ID
    // ========================
    private static final String FIND_BY_ID = """
            SELECT id         as chat_id,
                   type       as chat_type,
                   name       as chat_name,
                   avatar_url as chat_avatar_url
            FROM chats
            WHERE id = ?
            """;

    @Override
    public Optional<Chat> findById(Long chatId) {
        List<Chat> result = jdbcTemplate.query(
                FIND_BY_ID,
                (rs, rowNum) -> {
                    Chat chat = new Chat();
                    chat.setId(rs.getLong("chat_id"));
                    chat.setType(rs.getString("chat_type"));
                    chat.setName(rs.getString("chat_name"));
                    chat.setAvatarUrl(rs.getString("chat_avatar_url"));
                    return chat;
                },
                chatId
        );
        return result.stream().findFirst();
    }

    // ========================
    // СОЗДАТЬ ГРУППУ
    // ========================
    private static final String CREATE_GROUP_CHAT = """
        INSERT INTO chats (type, name, creator_id)
        VALUES ('group', ?, ?)
        """;

    @Override
    public Long createGroupChat(String name, Long creatorId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(
                    CREATE_GROUP_CHAT, PreparedStatement.RETURN_GENERATED_KEYS
            );
            stmt.setString(1, name);
            stmt.setLong(2, creatorId);
            return stmt;
        }, keyHolder);
        return ((Number) keyHolder.getKeys().get("id")).longValue();
    }

    // ========================
    // ОБНОВИТЬ ЧАТ
    // ========================
    private static final String UPDATE_CHAT = """
        UPDATE chats
        SET name       = COALESCE(?, name),
            avatar_url = COALESCE(?, avatar_url),
            updated_at = NOW()
        WHERE id = ?
        """;

    @Override
    public void updateChat(Long chatId, String name, String avatarUrl) {
        jdbcTemplate.update(UPDATE_CHAT, name, avatarUrl, chatId);
    }

    // ========================
    // УДАЛИТЬ ЧАТ
    // ========================
    private static final String DELETE_CHAT = """
        DELETE FROM chats WHERE id = ?
        """;

    @Override
    public void deleteChat(Long chatId) {
        jdbcTemplate.update(DELETE_CHAT, chatId);
    }

    // ========================
    // УДАЛИТЬ УЧАСТНИКА
    // ========================
    private static final String REMOVE_MEMBER = """
        DELETE FROM chat_members
        WHERE chat_id = ? AND user_id = ?
        """;

    @Override
    public void removeMember(Long chatId, Long userId) {
        jdbcTemplate.update(REMOVE_MEMBER, chatId, userId);
    }

    // ========================
    // РОЛЬ УЧАСТНИКА
    // ========================
    private static final String GET_MEMBER_ROLE = """
        SELECT role FROM chat_members
        WHERE chat_id = ? AND user_id = ?
        """;

    @Override
    public String getMemberRole(Long chatId, Long userId) {
        List<String> result = jdbcTemplate.query(
                GET_MEMBER_ROLE,
                (rs, rowNum) -> rs.getString("role"),
                chatId, userId
        );
        return result.isEmpty() ? null : result.get(0);
    }

    // ========================
    // ДОБАВИТЬ УЧАСТНИКА С РОЛЬЮ
    // ========================
    private static final String ADD_MEMBER_WITH_ROLE = """
        INSERT INTO chat_members (chat_id, user_id, role)
        VALUES (?, ?, ?)
        ON CONFLICT (chat_id, user_id) DO NOTHING
        """;

    @Override
    public void addMemberWithRole(Long chatId, Long userId, String role) {
        jdbcTemplate.update(ADD_MEMBER_WITH_ROLE, chatId, userId, role);
    }
}