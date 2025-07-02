package org.kosign.chatbotapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableRetry
@ConfigurationPropertiesScan("org.kosign.chatbotapi.properties")
public class ChatBotApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatBotApiApplication.class, args);
	}
}