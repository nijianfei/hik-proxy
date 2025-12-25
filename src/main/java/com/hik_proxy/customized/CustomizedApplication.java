package com.hik_proxy.customized;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CustomizedApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustomizedApplication.class, args);
	}

}
