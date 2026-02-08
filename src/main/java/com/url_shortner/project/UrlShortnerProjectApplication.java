package com.url_shortner.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.concurrent.CompletableFuture;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class UrlShortnerProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(UrlShortnerProjectApplication.class, args);
	}

}
