package com.rgr.messanger.service;

import com.rgr.messanger.entity.user.Role;
import com.rgr.messanger.web.dto.admin.AdminStatsResponse;
import com.rgr.messanger.web.dto.admin.AdminUserResponse;

import java.util.List;
import java.util.Set;

public interface AdminService {
    List<AdminUserResponse> getAllUsers();
    void setBlocked(Long userId, boolean blocked, Long currentAdminId);
    void updateRoles(Long userId, Set<Role> roles, Long currentAdminId);
    AdminStatsResponse getStats();
}