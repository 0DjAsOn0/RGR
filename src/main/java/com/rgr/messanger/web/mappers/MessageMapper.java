package com.rgr.messanger.web.mappers;

import com.rgr.messanger.entity.message.Message;
import com.rgr.messanger.web.dto.message.MessageDto;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MessageMapper {

    MessageDto toDto(Message message);

    List<MessageDto> toDto(List<Message> messages);

    Message toEntity(MessageDto dto);
}
