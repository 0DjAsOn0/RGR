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

    private static final String ACCESS_TOKEN_COOKIE = "accessToken";

    private final JwtTokenProvider jwtTokenProvider;

    // ========================
    // КОРЕНЬ
    // ========================

    @GetMapping("/")
    public String index(HttpServletRequest request) {
        return isAuthenticated(request)
                ? "redirect:/chat"
                : "redirect:/login";
    }

    // ========================
    // ПУБЛИЧНЫЕ СТРАНИЦЫ
    // ========================

    @GetMapping("/login")
    public String login(HttpServletRequest request) {
        return isAuthenticated(request)
                ? "redirect:/chat"
                : "public/login";
    }

    @GetMapping("/register")
    public String register(HttpServletRequest request) {
        return isAuthenticated(request)
                ? "redirect:/chat"
                : "public/register";
    }

    @GetMapping("/passreset")
    public String passreset(HttpServletRequest request) {
        return isAuthenticated(request)
                ? "redirect:/chat"
                : "public/passreset";
    }

    @GetMapping("/check-email")
    public String checkEmail() {
        return "public/check-email";
    }

    // ========================
    // ПРИВАТНЫЕ СТРАНИЦЫ
    // ========================

    @GetMapping("/chat")
    public String chat(HttpServletRequest request) {
        return isAuthenticated(request)
                ? "private/chat"
                : "redirect:/login";
    }

    // ========================
    // УТИЛИТЫ
    // ========================

    private boolean isAuthenticated(HttpServletRequest request) {
        String token = resolveToken(request);
        return token != null && jwtTokenProvider.isValid(token);
    }

    private String resolveToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        for (Cookie cookie : cookies) {
            if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}