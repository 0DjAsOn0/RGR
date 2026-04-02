package com.rgr.messanger.repository.impl;

import com.rgr.messanger.entity.message.Message;
import com.rgr.messanger.repository.DataSourceConfig;
import com.rgr.messanger.repository.MessageRepo;
import com.rgr.messanger.web.mappers.MessageRowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class MessageRepoImpl implements MessageRepo {

    private final DataSourceConfig dataSourceConfig;

    private final String FIND_BY_ID = """
            SELECT m.id as message_id,
                   m.text as message_text,
                   m.status as message_status,
                   m.send_date as message_send_date
            FROM messages m
            WHERE id = ?
            """;

    private final String FIND_ALL_BY_USER_ID = """
            SELECT m.id as message_id,
                   m.chat_id as message_chat_id,
                   m.sender_id as message_sender_id,
                   m.reply_to_id as message_reply_to_id,
                   m.type as message_type,
                   m.text as message_text,
                   m.is_edited as message_is_edited,
                   m.is_deleted as message_is_deleted,
                   m.send_date as message_send_date,
                   m.edited_at as message_edited_at,
                   m.status as message_status
            FROM messages m
            JOIN chat_members cm ON m.chat_id = cm.chat_id
            WHERE cm.user_id = ?
            ORDER BY m.send_date DESC
            """;

    private final String ASSIGN_TO_USER_BY_ID = """
            INSERT INTO chat_members (chat_id, user_id, role)
            SELECT m.chat_id, ?, 'member'
            FROM messages m
            WHERE m.id = ?
            ON CONFLICT (chat_id, user_id) DO NOTHING
            """;

    private final String CREATE = """
            INSERT INTO messages (chat_id, sender_id, reply_to_id, type, text, status)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private final String UPDATE = """
            UPDATE messages
            SET text = ?,
                status = ?,
                is_edited = true,
                edited_at = NOW()
            WHERE id = ?
            """;

    private final String DELETE = """
            DELETE FROM messages
            WHERE id = ?
            """;

    public MessageRepoImpl(DataSourceConfig dataSourceConfig) {
        this.dataSourceConfig = dataSourceConfig;
    }

    @Override
    public Optional<Message> findById(Long id) {

        try {
            Connection connection = dataSourceConfig.getConnection();
            PreparedStatement stmt = connection.prepareStatement(FIND_BY_ID);
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return Optional.ofNullable(MessageRowMapper.mapRow(rs));
            }

        } catch (SQLException throwable) {
            throw new RuntimeException("Error finding message by id");
        }
    }

    @Override
    public List<Message> findAllByUserId(Long userId) {

        try {
            Connection connection = dataSourceConfig.getConnection();
            PreparedStatement stmt = connection.prepareStatement(FIND_ALL_BY_USER_ID);
            stmt.setLong(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                return MessageRowMapper.mapRows(rs);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error finding all by message by id", e);
        }
    }

    @Override
    public void assignToUserById(Long userId, Long messageId) {
        try {
            Connection connection = dataSourceConfig.getConnection();
            PreparedStatement stmt = connection.prepareStatement(ASSIGN_TO_USER_BY_ID);
            stmt.setLong(1, messageId);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        } catch (SQLException throwable) {
            throw new RuntimeException("Error while assigning to user");
        }
    }

    @Override
    public void create(Message message) {
        try {
            Connection connection = dataSourceConfig.getConnection();
            PreparedStatement stmt = connection.prepareStatement(CREATE, PreparedStatement.RETURN_GENERATED_KEYS);
            stmt.setString(1, message.getText());
            stmt.setString(2, message.getStatus().name());
            stmt.setLong(3, message.getId());
            stmt.executeUpdate();
            try(ResultSet rs = stmt.getGeneratedKeys()){
                rs.next();
                message.setId(rs.getLong(1));
            }
        } catch (SQLException throwable) {
            throw new RuntimeException("Error while create message");
        }
    }

    @Override
    public void delete(Long id) {
        try {
            Connection connection = dataSourceConfig.getConnection();
            PreparedStatement stmt = connection.prepareStatement(DELETE);
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException throwable) {
            throw new RuntimeException("Error while deleting message");
        }
    }

    @Override
    public void update(Message message) {
        try {
            Connection connection = dataSourceConfig.getConnection();
            PreparedStatement stmt = connection.prepareStatement(UPDATE);
            stmt.setString(1, message.getText());
            stmt.setString(2, message.getStatus().name());
            stmt.setLong(3, message.getId());

            stmt.executeUpdate();
        } catch (SQLException throwable) {
            throw new RuntimeException("Error while updating message");
        }
    }
}
