package org.kosign.chatbotapi;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableBatchProcessing
@EnableScheduling
public class ChatBotApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatBotApiApplication.class, args);
	}
}