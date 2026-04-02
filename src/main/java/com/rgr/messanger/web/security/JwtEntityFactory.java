package com.rgr.messanger.web.security;

import com.rgr.messanger.entity.user.Role;
import com.rgr.messanger.entity.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class JwtEntityFactory {
    public static JwtEntity create(User user){
        return new JwtEntity(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                mapToGrantedAuthorities(user.getRoles() == null
                        ? Collections.emptyList()
                        : user.getRoles().stream().toList())
        );
    }
    private static List<GrantedAuthority> mapToGrantedAuthorities(List<Role> roles){
        return roles.stream().map(Enum::name).map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }
}
