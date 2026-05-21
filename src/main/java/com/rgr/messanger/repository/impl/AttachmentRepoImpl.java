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
import java.sql.Types;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AttachmentRepoImpl implements AttachmentRepo {

    private final JdbcTemplate jdbcTemplate;

    // ========================
    // СОХРАНИТЬ
    // ========================
    private static final String SAVE = """
        INSERT INTO attachments (message_id, file_url, file_name, file_size, mime_type)
        VALUES (?, ?, ?, ?, ?)
        """;

    @Override
    public void save(Attachment attachment) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(
                    SAVE, new String[]{"id"}
            );
            stmt.setLong(1, attachment.getMessageId());
            stmt.setString(2, attachment.getFileUrl());
            stmt.setString(3, attachment.getFileName());

            if (attachment.getFileSize() != null) {
                stmt.setLong(4, attachment.getFileSize());
            } else {
                stmt.setNull(4, Types.BIGINT);
            }

            stmt.setString(5, attachment.getMimeType());
            return stmt;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to retrieve generated attachment id");
        }
        attachment.setId(key.longValue());
    }

    // ========================
    // ПОЛУЧИТЬ ПО СООБЩЕНИЮ
    // ========================
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
            a.setFileSize(rs.getObject("file_size", Long.class));
            a.setMimeType(rs.getString("mime_type"));

            Timestamp ts = rs.getTimestamp("created_at");
            if (ts != null) a.setCreatedAt(ts.toLocalDateTime());

            return a;
        }, messageId);
    }

    // ========================
    // УДАЛИТЬ ПО СООБЩЕНИЮ
    // ========================
    private static final String DELETE_BY_MESSAGE_ID = """
        DELETE FROM attachments WHERE message_id = ?
        """;

    @Override
    public void deleteByMessageId(Long messageId) {
        jdbcTemplate.update(DELETE_BY_MESSAGE_ID, messageId);
    }
}