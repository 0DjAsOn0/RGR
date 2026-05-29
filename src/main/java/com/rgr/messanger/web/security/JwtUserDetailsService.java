package com.rgr.messanger.web.security;

import com.rgr.messanger.entity.user.User;
import com.rgr.messanger.exception.ResourceNotFoundException;
import com.rgr.messanger.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

//получить пользователя для аутентификации
public class JwtUserDetailsService implements UserDetailsService {

    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            User user = userService.getByUsername(username);

            if (user.isBlocked()) {
                throw new LockedException("Пользователь заблокирован");
            }

            return JwtEntityFactory.create(user);
        } catch (ResourceNotFoundException e) {
            throw new UsernameNotFoundException(
                    "Пользователь не найден: " + username, e
            );
        }
    }
}