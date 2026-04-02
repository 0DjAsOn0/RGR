package com.rgr.messanger.controller;

import com.rgr.messanger.entity.user.User;
import com.rgr.messanger.repository.UserRepo;
import com.rgr.messanger.service.impl.EmailServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Controller
public class MainController {

    private final UserRepo userRepo;
    private final ObjectMapper objectMapper;
    private final EmailServiceImpl emailServiceImpl;

    @Autowired
    public MainController(UserRepo userRepo,
                          ObjectMapper objectMapper,
                          EmailServiceImpl emailServiceImpl) {
        this.userRepo = userRepo;
        this.objectMapper = objectMapper;
        this.emailServiceImpl = emailServiceImpl;
    }


    @GetMapping("/hello")
    public ResponseEntity<User> sayHelloFromAdmin(@RequestParam long id) {
        try {
            var user = userRepo.findById(id).orElseThrow();
            emailServiceImpl.send(
                    "djeson948@gmail.com",
                    "ДОБРОЙ СМЕРТИ ГНИДА",
                    "Hello, my name is " + user.getUsername() + ". СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР! СДОХНИ ПИДОР!"
            );
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("Ошибка: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
