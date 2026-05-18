package com.rgr.messanger.controller;

import com.rgr.messanger.entity.user.User;
import com.rgr.messanger.service.AuthService;
import com.rgr.messanger.service.EmailVerificationService;
import com.rgr.messanger.service.PasswordResetService;
import com.rgr.messanger.service.UserService;
import com.rgr.messanger.web.dto.auth.*;
import com.rgr.messanger.web.dto.user.UserDto;
import com.rgr.messanger.web.dto.validation.OnCreate;
import com.rgr.messanger.web.mappers.UserMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final UserMapper userMapper;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @Validated(OnCreate.class) @RequestBody UserDto userDto
    ) {
        User user = userMapper.toEntity(userDto);
        userService.create(user);
        return ResponseEntity.ok(Map.of(
                "message", "Проверьте почту и подтвердите email"
        ));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(
            @RequestParam String token
    ) {
        emailVerificationService.verifyToken(token);
        return ResponseEntity.ok(Map.of(
                "message", "Email подтверждён! Теперь можно войти."
        ));
    }

    @PostMapping("/login")
    public JwtResponse login(
            @Validated @RequestBody final JwtRequest loginRequest,
            HttpServletResponse response
    ) {
        JwtResponse jwtResponse = authService.login(loginRequest);

        User user = userService.getByUsername(jwtResponse.getUsername());
        userService.updateOnlineStatus(user.getId(), true);

        int maxAge = loginRequest.isRememberMe()
                ? 60 * 60 * 24 * 30  // 30 дней
                : 60 * 60 * 24;      // 1 день

        Cookie cookie = new Cookie("accessToken", jwtResponse.getAccessToken());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
        return jwtResponse;
    }

    @PostMapping("/logout")
    public void logout(HttpServletResponse response, Principal principal) {

        if (principal != null) {
            User user = userService.getByUsername(principal.getName());
            userService.updateOnlineStatus(user.getId(), false);
        }

        Cookie cookie = new Cookie("accessToken", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    @PostMapping("/refresh")
    public JwtResponse refresh(@RequestBody final String refreshToken) {
        return authService.refresh(refreshToken);
    }

    // ========================
    //  отправка кода
    // ========================
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @RequestBody ForgotPasswordRequest request
    ) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email обязателен"));
        }

        passwordResetService.sendResetCode(request.getEmail());

        return ResponseEntity.ok(Map.of("message", "Код отправлен на почту"));
    }

    // ========================
    // проверка кода
    // ========================
    @PostMapping("/verify-reset-code")
    public ResponseEntity<Map<String, String>> verifyResetCode(
            @RequestBody VerifyResetCodeRequest request
    ) {
        if (request.getEmail() == null || request.getCode() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email и код обязательны"));
        }

        boolean valid = passwordResetService.verifyCode(
                request.getEmail(), request.getCode()
        );

        if (!valid) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Неверный или истёкший код"));
        }

        return ResponseEntity.ok(Map.of("message", "Код подтверждён"));
    }

    // ========================
    //  смена пароля
    // ========================
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestBody ResetPasswordRequest request
    ) {
        if (request.getNewPassword() == null
                || request.getNewPassword().length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Пароль не менее 6 символов"));
        }

        if (!request.getNewPassword().equals(request.getNewPasswordConfirmation())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Пароли не совпадают"));
        }

        try {
            passwordResetService.resetPassword(
                    request.getEmail(), request.getNewPassword()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }

        return ResponseEntity.ok(Map.of("message", "Пароль успешно изменён"));
    }
}

