package com.rgr.messanger.web.mappers;

import com.rgr.messanger.entity.message.Message;
import com.rgr.messanger.entity.message.Status;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;

public class MessageRowMapper {

    private MessageRowMapper() {} // utility-класс

    /**
     * Маппит одну строку ResultSet в Message.
     * Не вызывает rs.next() — это ответственность вызывающего кода.
     */
    public static Message extractMessage(ResultSet rs) throws SQLException {
        Message message = new Message();
        message.setId(rs.getLong("message_id"));
        message.setChatId(rs.getLong("message_chat_id"));

        // sender_id может быть NULL (если юзер удалён)
        message.setSenderId(rs.getObject("message_sender_id", Long.class));

        // sender_name — опциональная колонка (есть только при JOIN с users)
        if (hasColumn(rs, "sender_name")) {
            message.setSenderName(rs.getString("sender_name"));
        }

        message.setText(rs.getString("message_text"));
        message.setType(rs.getString("message_type"));
        message.setEdited(rs.getBoolean("message_is_edited"));
        message.setDeleted(rs.getBoolean("message_is_deleted"));

        Long replyToId = rs.getObject("message_reply_to_id", Long.class);
        message.setReplyToId(replyToId);

        String status = rs.getString("message_status");
        message.setStatus(
                status != null ? Status.valueOf(status) : Status.NOT_SENDING
        );

        Timestamp sendDate = rs.getTimestamp("message_send_date");
        if (sendDate != null) message.setSendDate(sendDate.toLocalDateTime());

        Timestamp editedAt = rs.getTimestamp("message_edited_at");
        if (editedAt != null) message.setEditedAt(editedAt.toLocalDateTime());

        return message;
    }

    /**
     * Для ResultSetExtractor: проверяет наличие строки и маппит её.
     */
    public static Message mapRow(ResultSet rs) throws SQLException {
        if (!rs.next()) return null;
        return extractMessage(rs);
    }

    /**
     * Проверяет, есть ли колонка в результате SELECT.
     * Нужно, чтобы один mapper работал и для запросов с JOIN, и без.
     */
    private static boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int count = meta.getColumnCount();
        for (int i = 1; i <= count; i++) {
            if (columnName.equalsIgnoreCase(meta.getColumnLabel(i))) {
                return true;
            }
        }
        return false;
    }
}