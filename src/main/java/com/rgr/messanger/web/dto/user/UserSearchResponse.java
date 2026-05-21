package com.rgr.messanger.web.dto.user;

import com.rgr.messanger.entity.user.User;

public record UserSearchResponse(
        Long   id,
        String username,
        String avatarUrl,
        String status
) {
    public static UserSearchResponse from(User user) {
        return new UserSearchResponse(
                user.getId(),
                user.getUsername(),
                user.getAvatarUrl(),
                user.getStatus()
        );
    }
}