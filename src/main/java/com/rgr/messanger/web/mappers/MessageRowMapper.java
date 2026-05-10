package com.rgr.messanger.web.mappers;

import com.rgr.messanger.entity.message.Message;
import com.rgr.messanger.entity.message.Status;
import lombok.SneakyThrows;

import java.sql.ResultSet;
import java.sql.Timestamp;

public class MessageRowMapper {

    @SneakyThrows
    public static Message mapRow(ResultSet rs) {
        if (!rs.next()) return null;
        return extractMessage(rs);
    }

    @SneakyThrows
    public static Message extractMessage(ResultSet rs) {
        Message message = new Message();
        message.setId(rs.getLong("message_id"));
        message.setChatId(rs.getLong("message_chat_id"));
        message.setSenderId(rs.getLong("message_sender_id"));
        message.setText(rs.getString("message_text"));
        message.setType(rs.getString("message_type"));
        message.setEdited(rs.getBoolean("message_is_edited"));
        message.setDeleted(rs.getBoolean("message_is_deleted"));

        long replyToId = rs.getLong("message_reply_to_id");
        if (!rs.wasNull()) message.setReplyToId(replyToId);

        String status = rs.getString("message_status");
        message.setStatus(status != null ? Status.valueOf(status) : Status.NOT_SENDING);

        Timestamp sendDate = rs.getTimestamp("message_send_date");
        if (sendDate != null) message.setSendDate(sendDate.toLocalDateTime());

        Timestamp editedAt = rs.getTimestamp("message_edited_at");
        if (editedAt != null) message.setEditedAt(editedAt.toLocalDateTime());

        return message;
    }
}