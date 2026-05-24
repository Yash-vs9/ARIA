package com.yash.nerve;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class NerveApplication {

	public static void main(String[] args) {
		SpringApplication.run(NerveApplication.class, args);
	}

}
