package com.rgr.messanger.entity.user;

import com.rgr.messanger.entity.message.Message;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
public class User {
    private Long id;
    private String username;
    private String email;
    private String password;
    private String passwordConfirmation;
    private String avatarUrl;
    private String status;
    private LocalDateTime lastSeen;
    private boolean online;
    private Set<Role> roles;
    private List<Message> message;
}