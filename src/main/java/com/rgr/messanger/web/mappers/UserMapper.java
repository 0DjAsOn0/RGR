package com.rgr.messanger.web.mappers;

import com.rgr.messanger.entity.user.User;
import com.rgr.messanger.web.dto.user.UserDto;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE  // не ругаться на отсутствующие в DTO поля
)
public interface UserMapper {

    @Mapping(target = "password", ignore = true)
    @Mapping(target = "passwordConfirmation", ignore = true)
    UserDto toDto(User user);

    @Mapping(target = "avatarUrl",           ignore = true)
    @Mapping(target = "status",              ignore = true)
    @Mapping(target = "lastSeen",            ignore = true)
    @Mapping(target = "emailVerified",       ignore = true)
    @Mapping(target = "emailNotifications",  ignore = true)
    @Mapping(target = "roles",               ignore = true)
    @Mapping(target = "createdAt",           ignore = true)
    @Mapping(target = "updatedAt",           ignore = true)
    User toEntity(UserDto dto);
}