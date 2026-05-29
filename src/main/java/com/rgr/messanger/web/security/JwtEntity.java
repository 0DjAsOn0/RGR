package com.rgr.messanger.web.security;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;


//пользователь в формате безопасности Spring Security
@Getter
@ToString(exclude = "password")
@EqualsAndHashCode(of = "id")
public class JwtEntity implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final boolean blocked;
    private final Collection<? extends GrantedAuthority> authorities;

    public JwtEntity(
            Long id,
            String username,
            String password,
            boolean blocked,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.blocked = blocked;
        this.authorities = authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !blocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !blocked;
    }
}