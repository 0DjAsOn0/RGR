package com.rgr.messanger.web.mappers;

import com.rgr.messanger.entity.user.User;
import com.rgr.messanger.web.dto.user.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "password", ignore = true)
    @Mapping(target = "passwordConfirmation", ignore = true)
    UserDto toDto(User user);

    User toEntity(UserDto dto);

}
