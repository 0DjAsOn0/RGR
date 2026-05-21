package com.rgr.messanger.entity.user;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@ToString(exclude = "password")
@EqualsAndHashCode(of = "id")
public class User {
    private Long id;
    private String username;
    private String email;
    private String password;
    private String avatarUrl;
    private String status;
    private LocalDateTime lastSeen;
    private boolean emailVerified;
    private boolean emailNotifications;
    private Set<Role> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}