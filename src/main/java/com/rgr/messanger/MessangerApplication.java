package com.rgr.messanger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication	//главная аннотация запуска Spring Boot приложения
@EnableTransactionManagement	//автоматически настроить приложение по зависимостям и настройкам
@EnableAsync	//асинхронное выполнение методов
@EnableScheduling	//поддержку планировщика задач
public class MessangerApplication {
	public static void main(String[] args) {
		SpringApplication.run(MessangerApplication.class, args);
	}
}
