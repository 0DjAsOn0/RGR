package com.rgr.messanger.web.dto.admin;

import com.rgr.messanger.entity.user.Role;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class AdminUserResponse {

    private Long id;
    private String username;
    private String email;
    private String avatarUrl;
    private String status;
    private LocalDateTime lastSeen;
    private boolean emailVerified;
    private boolean emailNotifications;
    private boolean blocked;
    private Set<Role> roles;
    private LocalDateTime createdAt;
}