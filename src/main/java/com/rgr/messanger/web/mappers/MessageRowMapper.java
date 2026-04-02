package com.rgr.messanger.web.mappers;

import com.rgr.messanger.entity.message.Message;
import com.rgr.messanger.entity.message.Status;
import lombok.SneakyThrows;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class MessageRowMapper {

    @SneakyThrows
    public static Message mapRow(ResultSet resultSet){
        if (resultSet.next()){
            Message message = new Message();
            message.setId(resultSet.getLong("message_id"));
            message.setText(resultSet.getString("message_text"));
            message.setStatus(Status.valueOf(resultSet.getString("message_status")));
            Timestamp timestamp = resultSet.getTimestamp("send_date");
            if (timestamp != null){
                message.setSendDate(timestamp.toLocalDateTime());
            }

            return message;
        }
        return null;
    }

    @SneakyThrows
    public static List<Message> mapRows(ResultSet resultSet) {
        List<Message> messages = new ArrayList<>();
        while (resultSet.next()) {
            Message message = new Message();
            message.setId(resultSet.getLong("message_id"));
            if (!resultSet.wasNull()) {
                message.setText(resultSet.getString("message_text"));
                message.setStatus(Status.valueOf(resultSet.getString("message_status")));
                Timestamp timestamp = resultSet.getTimestamp("send_date");
                if (timestamp != null) {
                    message.setSendDate(timestamp.toLocalDateTime());
                }
                messages.add(message);
            }
        }
        return messages;
    }
}
