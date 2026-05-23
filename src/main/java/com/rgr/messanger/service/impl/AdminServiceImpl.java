package com.rgr.messanger.service.impl;

import com.rgr.messanger.entity.user.Role;
import com.rgr.messanger.entity.user.User;
import com.rgr.messanger.repository.AdminRepo;
import com.rgr.messanger.repository.UserRepo;
import com.rgr.messanger.service.AdminService;
import com.rgr.messanger.web.dto.admin.AdminStatsResponse;
import com.rgr.messanger.web.dto.admin.AdminUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepo userRepo;
    private final AdminRepo adminRepo;

    @Override
    public List<AdminUserResponse> getAllUsers() {
        return userRepo.findAll().stream()
                .map(this::toAdminUserResponse)
                .toList();
    }

    @Override
    public void setBlocked(Long userId, boolean blocked, Long currentAdminId) {
        if (userId.equals(currentAdminId)) {
            throw new IllegalArgumentException("Нельзя заблокировать самого себя");
        }

        userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        userRepo.updateBlocked(userId, blocked);
    }

    @Override
    public void updateRoles(Long userId, Set<Role> roles, Long currentAdminId) {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("Нужно указать хотя бы одну роль");
        }

        if (userId.equals(currentAdminId)) {
            throw new IllegalArgumentException("Нельзя менять роли самому себе");
        }

        userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        userRepo.replaceUserRoles(userId, roles.stream().toList());
    }

    @Override
    public AdminStatsResponse getStats() {
        return new AdminStatsResponse(
                userRepo.countUsers(),
                adminRepo.countMessages()
        );
    }

    private AdminUserResponse toAdminUserResponse(User user) {
        AdminUserResponse dto = new AdminUserResponse();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setStatus(user.getStatus());
        dto.setLastSeen(user.getLastSeen());
        dto.setEmailVerified(user.isEmailVerified());
        dto.setEmailNotifications(user.isEmailNotifications());
        dto.setBlocked(user.isBlocked());
        dto.setRoles(user.getRoles());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}