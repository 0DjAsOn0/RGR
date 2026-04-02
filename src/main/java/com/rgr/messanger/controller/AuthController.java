package com.rgr.messanger.controller;

import com.rgr.messanger.entity.user.User;
import com.rgr.messanger.service.AuthService;
import com.rgr.messanger.service.UserService;
import com.rgr.messanger.web.dto.auth.JwtRequest;
import com.rgr.messanger.web.dto.auth.JwtResponse;
import com.rgr.messanger.web.dto.user.UserDto;
import com.rgr.messanger.web.dto.validation.OnCreate;
import com.rgr.messanger.web.mappers.UserMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final UserMapper userMapper;

    @PostMapping("/register")
    public UserDto register(
            @Validated(OnCreate.class)
            @RequestBody final UserDto userDto
    ) {
        User user = userMapper.toEntity(userDto);
        User createdUser = userService.create(user);
        return userMapper.toDto(createdUser);
    }

    @PostMapping("/login")
    public JwtResponse login(
            @Validated @RequestBody final JwtRequest loginRequest,
            HttpServletResponse response
    ) {
        JwtResponse jwtResponse = authService.login(loginRequest);

        Cookie cookie = new Cookie("accessToken", jwtResponse.getAccessToken());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24);
        response.addCookie(cookie);

        return jwtResponse;
    }

    @PostMapping("/logout")
    public void logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("accessToken", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    @PostMapping("/refresh")
    public JwtResponse refresh(
            @RequestBody final String refreshToken
    ) {
        return authService.refresh(refreshToken);
    }

}

