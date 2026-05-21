package com.rgr.messanger.web.dto.user;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String username;
    private String avatarUrl;
    private String oldPassword;
    private String password;
}