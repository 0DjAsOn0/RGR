package com.rgr.messanger.web.dto.admin;

import com.rgr.messanger.entity.user.Role;
import lombok.Data;

import java.util.Set;

@Data
public class UpdateUserRolesRequest {
    private Set<Role> roles;
}