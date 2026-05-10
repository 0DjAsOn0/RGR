package com.rgr.messanger.repository.impl;

import com.rgr.messanger.entity.message.Message;
import com.rgr.messanger.entity.message.Status;
import com.rgr.messanger.repository.MessageRepo;
import com.rgr.messanger.web.mappers.MessageRowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MessageRepoImpl implements MessageRepo {

    private final JdbcTemplate jdbcTemplate;

    // ========================
    // НАЙТИ СООБЩЕНИЕ ПО ID
    // ========================
    private static final String FIND_BY_ID = """
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
            FROM messages m
            WHERE m.id = ?
            """;

    @Override
    public Optional<Message> findById(Long id) {
        Message message = jdbcTemplate.query(
                FIND_BY_ID,
                (ResultSetExtractor<Message>) rs -> MessageRowMapper.mapRow(rs),
                id
        );
        return Optional.ofNullable(message);
    }

    // ========================
    // НАЙТИ ВСЕ СООБЩЕНИЯ ЧАТА
    // ========================
    private static final String FIND_BY_CHAT_ID = """
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
            FROM messages m
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
    private static final String FIND_ALL_BY_USER_ID = """
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


    private static final String UPDATE_STATUS = """
        UPDATE messages
        SET status = ?
        WHERE id = ?
        """;

    @Override
    public void updateStatus(Long messageId, Status status) {
        jdbcTemplate.update(UPDATE_STATUS, status.name(), messageId);
    }


    @Override
    public void create(Message message) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(
                    CREATE, PreparedStatement.RETURN_GENERATED_KEYS
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
                    ? message.getStatus().name() : Status.SENDING.name());

            return stmt;
        }, keyHolder);

        if (keyHolder.getKeys() != null) {
            message.setId(((Number) keyHolder.getKeys().get("id")).longValue());
        }
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
    // УДАЛИТЬ СООБЩЕНИЕ (мягкое)
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
    // ДОБАВИТЬ УЧАСТНИКА В ЧАТ
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
}