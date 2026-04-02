package com.rgr.messanger.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "public/login";
    }

    @GetMapping("/passreset")
    public String passreset() {
        return "public/passreset";
    }

    @GetMapping("/chat")
    public String chat() {
        return "private/chat";
    }

    @GetMapping("/register")
    public String register() {
        return "public/register";
    }
}
