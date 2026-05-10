package com.rgr.messanger.controller;

import com.rgr.messanger.web.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final JwtTokenProvider jwtTokenProvider;

    // ========================
    // КОРЕНЬ
    // ========================

    @GetMapping("/")
    public String index(HttpServletRequest request) {
        // Авторизован → чат, иначе → логин
        if (isAuthenticated(request)) {
            return "redirect:/chat";
        }
        return "redirect:/login";
    }

    // ========================
    // ПУБЛИЧНЫЕ СТРАНИЦЫ
    // (авторизованных → на /chat)
    // ========================

    @GetMapping("/login")
    public String login(HttpServletRequest request) {
        if (isAuthenticated(request)) {
            return "redirect:/chat";
        }
        return "public/login";
    }

    @GetMapping("/register")
    public String register(HttpServletRequest request) {
        if (isAuthenticated(request)) {
            return "redirect:/chat";
        }
        return "public/register";
    }

    @GetMapping("/passreset")
    public String passreset(HttpServletRequest request) {
        if (isAuthenticated(request)) {
            return "redirect:/chat";
        }
        return "public/passreset";
    }

    @GetMapping("/check-email")
    public String checkEmail() {
        return "public/check-email";
    }

    // ========================
    // ПРИВАТНЫЕ СТРАНИЦЫ
    // (не авторизованных → на /login)
    // ========================

    @GetMapping("/chat")
    public String chat(HttpServletRequest request) {
        if (!isAuthenticated(request)) {
            return "redirect:/login";
        }
        return "private/chat";
    }

    // ========================
    // УТИЛИТЫ
    // ========================

    private boolean isAuthenticated(HttpServletRequest request) {
        String token = resolveToken(request);
        return token != null && jwtTokenProvider.isValid(token);
    }

    private String resolveToken(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if ("accessToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}