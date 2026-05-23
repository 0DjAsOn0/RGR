package com.rgr.messanger.repository.impl;

import com.rgr.messanger.entity.message.Message;
import com.rgr.messanger.entity.message.Status;
import com.rgr.messanger.repository.MessageRepo;
import com.rgr.messanger.web.mappers.MessageRowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MessageRepoImpl implements MessageRepo {

    private final JdbcTemplate jdbcTemplate;

    // ========================
    // БАЗОВЫЕ КОЛОНКИ
    // ========================
    private static final String SELECT_BASE = """
        SELECT m.id          as message_id,
               m.chat_id     as message_chat_id,
               m.sender_id   as message_sender_id,
               m.reply_to_id as message_reply_to_id,
               m.type        as message_type,
               m.text        as message_text,
               m.is_edited   as message_is_edited,
               m.is_deleted  as message_is_deleted,
               m.send_date   as message_send_date,
               m.edited_at   as message_edited_at,
               m.status      as message_status
        """;

    // ========================
    // НАЙТИ ПО ID
    // ========================
    private static final String FIND_BY_ID =
            SELECT_BASE + " FROM messages m WHERE m.id = ?";

    @Override
    public Optional<Message> findById(Long id) {
        Message message = jdbcTemplate.query(
                FIND_BY_ID,
                MessageRowMapper::mapRow,
                id
        );
        return Optional.ofNullable(message);
    }

    // ========================
    // НАЙТИ ВСЕ СООБЩЕНИЯ ЧАТА
    // ========================
    private static final String FIND_BY_CHAT_ID = SELECT_BASE + """
        ,u.username as sender_name
        FROM messages m
        LEFT JOIN users u ON u.id = m.sender_id
        WHERE m.chat_id = ?
          AND m.is_deleted = FALSE
        ORDER BY m.send_date ASC
        """;

    @Override
    public List<Message> findByChatId(Long chatId) {
        return jdbcTemplate.query(
                FIND_BY_CHAT_ID,
                (rs, rowNum) -> MessageRowMapper.extractMessage(rs),
                chatId
        );
    }

    // ========================
    // НАЙТИ ВСЕ СООБЩЕНИЯ ПОЛЬЗОВАТЕЛЯ
    // ========================
    private static final String FIND_ALL_BY_USER_ID = SELECT_BASE + """
        FROM messages m
        JOIN chat_members cm ON m.chat_id = cm.chat_id
        WHERE cm.user_id = ?
          AND m.is_deleted = FALSE
        ORDER BY m.send_date DESC
        """;

    @Override
    public List<Message> findAllByUserId(Long userId) {
        return jdbcTemplate.query(
                FIND_ALL_BY_USER_ID,
                (rs, rowNum) -> MessageRowMapper.extractMessage(rs),
                userId
        );
    }

    // ========================
    // СОЗДАТЬ СООБЩЕНИЕ
    // ========================
    private static final String CREATE = """
        INSERT INTO messages (chat_id, sender_id, reply_to_id, type, text, status)
        VALUES (?, ?, ?, ?, ?, ?)
        """;

    @Override
    public void create(Message message) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(
                    CREATE, new String[]{"id"}
            );
            stmt.setLong(1, message.getChatId());
            stmt.setLong(2, message.getSenderId());

            if (message.getReplyToId() != null) {
                stmt.setLong(3, message.getReplyToId());
            } else {
                stmt.setNull(3, Types.BIGINT);
            }

            stmt.setString(4, message.getType() != null
                    ? message.getType() : "text");
            stmt.setString(5, message.getText());
            stmt.setString(6, message.getStatus() != null
                    ? message.getStatus().name() : Status.SENT.name());

            return stmt;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to retrieve generated message id");
        }
        message.setId(key.longValue());
    }

    // ========================
    // ОБНОВИТЬ СТАТУС
    // ========================
    private static final String UPDATE_STATUS = """
        UPDATE messages
        SET status = ?
        WHERE id = ?
        """;

    @Override
    public void updateStatus(Long messageId, Status status) {
        jdbcTemplate.update(UPDATE_STATUS, status.name(), messageId);
    }

    // ========================
    // ОБНОВИТЬ СООБЩЕНИЕ
    // ========================
    private static final String UPDATE = """
        UPDATE messages
        SET text      = ?,
            status    = ?,
            is_edited = TRUE,
            edited_at = NOW()
        WHERE id = ?
        """;

    @Override
    public void update(Message message) {
        jdbcTemplate.update(UPDATE,
                message.getText(),
                message.getStatus() != null
                        ? message.getStatus().name()
                        : Status.SENT.name(),
                message.getId()
        );
    }

    // ========================
    // УДАЛИТЬ (мягко)
    // ========================
    private static final String DELETE = """
        UPDATE messages
        SET is_deleted = TRUE
        WHERE id = ?
        """;

    @Override
    public void delete(Long id) {
        jdbcTemplate.update(DELETE, id);
    }

    // ========================
    // ОТМЕТИТЬ КАК ПРОЧИТАННОЕ
    // ========================
    private static final String MARK_AS_READ = """
        INSERT INTO message_reads (chat_id, user_id, last_read_msg, last_read_at)
        VALUES (?, ?, (
            SELECT id FROM messages
            WHERE chat_id = ?
              AND is_deleted = FALSE
            ORDER BY send_date DESC
            LIMIT 1
        ), NOW())
        ON CONFLICT (chat_id, user_id) DO UPDATE
            SET last_read_msg = EXCLUDED.last_read_msg,
                last_read_at  = NOW()
        """;

    @Override
    public void markAsRead(Long chatId, Long userId) {
        jdbcTemplate.update(MARK_AS_READ, chatId, userId, chatId);
    }

    // ========================
    // ДОБАВИТЬ УЧАСТНИКА ПО СООБЩЕНИЮ
    // ========================
    private static final String ASSIGN_TO_USER_BY_ID = """
        INSERT INTO chat_members (chat_id, user_id, role)
        SELECT m.chat_id, ?, 'member'
        FROM messages m
        WHERE m.id = ?
        ON CONFLICT (chat_id, user_id) DO NOTHING
        """;

    @Override
    public void assignToUserById(Long userId, Long messageId) {
        jdbcTemplate.update(ASSIGN_TO_USER_BY_ID, userId, messageId);
    }

    // ========================
    // НЕПРОЧИТАННЫЕ ДЛЯ EMAIL-УВЕДОМЛЕНИЙ
    // ========================
    private static final int UNREAD_NOTIFICATION_THRESHOLD = 10;

    private static final String COUNT_UNREAD_PER_CHAT = """
        SELECT
            c.id                  as chat_id,
            c.name                as chat_name,
            COUNT(m.id)           as unread_count,
            u.id                  as user_id,
            u.email               as user_email,
            u.username            as user_username,
            u.email_notifications as email_notifications
        FROM messages m
        JOIN chats c ON c.id = m.chat_id
        JOIN chat_members cm ON cm.chat_id = c.id
        JOIN users u ON u.id = cm.user_id
        WHERE m.sender_id != cm.user_id
          AND m.is_deleted = FALSE
          AND m.id > COALESCE((
              SELECT last_read_msg FROM message_reads
              WHERE chat_id = c.id AND user_id = cm.user_id
          ), 0)
          AND u.email_notifications = TRUE
        GROUP BY c.id, c.name, u.id, u.email, u.username, u.email_notifications
        HAVING COUNT(m.id) >= ?
        """;

    @Override
    public List<Map<String, Object>> findChatsWithUnreadThreshold() {
        return jdbcTemplate.queryForList(
                COUNT_UNREAD_PER_CHAT, UNREAD_NOTIFICATION_THRESHOLD
        );
    }

    private static final String UPDATE_TEXT = """
        UPDATE messages
        SET text = ?, is_edited = TRUE, edited_at = NOW()
        WHERE id = ?
        """;

    @Override
    public void updateText(Long messageId, String text) {
        jdbcTemplate.update(UPDATE_TEXT, text, messageId);
    }

    private static final String MARK_DELETED = """
        UPDATE messages
        SET is_deleted = TRUE
        WHERE id = ?
        """;

    @Override
    public void markDeleted(Long messageId) {
        jdbcTemplate.update(MARK_DELETED, messageId);
    }

    // ========================
    // ОЧИСТИТЬ ВСЕ СООБЩЕНИЯ В ЧАТЕ (для заметок)
    // ========================
    private static final String MARK_DELETED_BY_CHAT_ID = """
        UPDATE messages
        SET is_deleted = TRUE
        WHERE chat_id = ?
          AND is_deleted = FALSE
        """;

    @Override
    public void markDeletedByChatId(Long chatId) {
        jdbcTemplate.update(MARK_DELETED_BY_CHAT_ID, chatId);
    }
}