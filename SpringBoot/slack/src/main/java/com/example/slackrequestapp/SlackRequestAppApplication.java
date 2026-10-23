package com.example.slackrequestapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync 
@EnableScheduling 
public class SlackRequestAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(SlackRequestAppApplication.class, args);
	}

}
