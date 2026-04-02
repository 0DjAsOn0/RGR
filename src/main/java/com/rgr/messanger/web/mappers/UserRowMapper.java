package com.rgr.messanger.web.mappers;

import com.rgr.messanger.entity.user.Role;
import com.rgr.messanger.entity.user.User;
import lombok.SneakyThrows;

import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

public class UserRowMapper {

    @SneakyThrows
    public static User mapRow(ResultSet resultSet){

        if (!resultSet.next()) {
            return null;
        }

        User user = new User();
        user.setId(resultSet.getLong("user_id"));
        user.setUsername(resultSet.getString("user_username"));
        user.setEmail(resultSet.getString("user_email"));
        user.setPassword(resultSet.getString("user_password"));
        user.setAvatarUrl(resultSet.getString("user_avatar_url"));
        Set<Role> roles = new HashSet<>();
        String role = resultSet.getString("user_role");

        if (role != null) {
            roles.add(Role.valueOf(role));
        }

        while (resultSet.next()) {
            String nextRole = resultSet.getString("user_role");
            if (nextRole != null) {
                roles.add(Role.valueOf(nextRole));
            }
        }

        user.setRoles(roles);

        return user;
    }
}

