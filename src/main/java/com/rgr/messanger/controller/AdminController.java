package com.rgr.messanger.controller;

import com.rgr.messanger.service.AdminService;
import com.rgr.messanger.web.dto.admin.AdminStatsResponse;
import com.rgr.messanger.web.dto.admin.AdminUserResponse;
import com.rgr.messanger.web.dto.admin.BlockUserRequest;
import com.rgr.messanger.web.dto.admin.UpdateUserRolesRequest;
import com.rgr.messanger.web.security.JwtEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public List<AdminUserResponse> getAllUsers() {
        return adminService.getAllUsers();
    }

    @PatchMapping("/users/{userId}/block")
    public void setBlocked(
            @PathVariable Long userId,
            @RequestBody BlockUserRequest request,
            @AuthenticationPrincipal JwtEntity admin
    ) {
        adminService.setBlocked(userId, request.isBlocked(), admin.getId());
    }

    @PatchMapping("/users/{userId}/roles")
    public void updateRoles(
            @PathVariable Long userId,
            @RequestBody UpdateUserRolesRequest request,
            @AuthenticationPrincipal JwtEntity admin
    ) {
        adminService.updateRoles(userId, request.getRoles(), admin.getId());
    }

    @GetMapping("/stats")
    public AdminStatsResponse getStats() {
        return adminService.getStats();
    }
}