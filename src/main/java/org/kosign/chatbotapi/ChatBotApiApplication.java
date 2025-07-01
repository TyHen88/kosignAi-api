package org.kosign.chatbotapi;

import org.kosign.chatbotapi.utils.PasswordUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableRetry
@ConfigurationPropertiesScan("org.kosign.chatbotapi.properties")
public class ChatBotApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatBotApiApplication.class, args);
	}
}