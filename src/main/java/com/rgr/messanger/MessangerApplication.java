package com.rgr.messanger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class MessangerApplication {

	public static void main(String[] args) {
        SpringApplication.run(MessangerApplication.class, args);
	}

}
