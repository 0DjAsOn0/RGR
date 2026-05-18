package com.rgr.messanger.repository.impl;

import com.rgr.messanger.entity.attachment.Attachment;
import com.rgr.messanger.repository.AttachmentRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AttachmentRepoImpl implements AttachmentRepo {

    private final JdbcTemplate jdbcTemplate;

    private static final String SAVE = """
            INSERT INTO attachments (message_id, file_url, file_name, file_size, mime_type, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    @Override
    public void save(Attachment attachment) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(
                    SAVE, PreparedStatement.RETURN_GENERATED_KEYS
            );
            stmt.setLong(1, attachment.getMessageId());
            stmt.setString(2, attachment.getFileUrl());
            stmt.setString(3, attachment.getFileName());
            stmt.setLong(4, attachment.getFileSize() != null ? attachment.getFileSize() : 0);
            stmt.setString(5, attachment.getMimeType());
            stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            return stmt;
        }, keyHolder);

        if (keyHolder.getKeys() != null) {
            attachment.setId(((Number) keyHolder.getKeys().get("id")).longValue());
        }
    }

    private static final String FIND_BY_MESSAGE_ID = """
            SELECT id, message_id, file_url, file_name, file_size, mime_type, created_at
            FROM attachments
            WHERE message_id = ?
            ORDER BY created_at ASC
            """;

    @Override
    public List<Attachment> findByMessageId(Long messageId) {
        return jdbcTemplate.query(FIND_BY_MESSAGE_ID, (rs, rowNum) -> {
            Attachment a = new Attachment();
            a.setId(rs.getLong("id"));
            a.setMessageId(rs.getLong("message_id"));
            a.setFileUrl(rs.getString("file_url"));
            a.setFileName(rs.getString("file_name"));
            a.setFileSize(rs.getLong("file_size"));
            a.setMimeType(rs.getString("mime_type"));
            Timestamp ts = rs.getTimestamp("created_at");
            if (ts != null) a.setCreatedAt(ts.toLocalDateTime());
            return a;
        }, messageId);
    }

    private static final String DELETE_BY_MESSAGE_ID = """
            DELETE FROM attachments WHERE message_id = ?
            """;

    @Override
    public void deleteByMessageId(Long messageId) {
        jdbcTemplate.update(DELETE_BY_MESSAGE_ID, messageId);
    }
}
