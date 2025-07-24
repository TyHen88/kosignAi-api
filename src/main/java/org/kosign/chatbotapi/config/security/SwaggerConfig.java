//package org.kosign.chatbotapi.config.security;
//
//import io.swagger.v3.oas.models.OpenAPI;
//import io.swagger.v3.oas.models.info.Info;
//import io.swagger.v3.oas.models.security.SecurityScheme;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class SwaggerConfig {
//
//    @Bean
//    public OpenAPI customOpenAPI() {
//        return new OpenAPI()
//                .info(new Info()
//                        .title("ChatBot AI API Documentation")
//                        .version("1.0")
//                        .description("API documentation for your application"))
//                .schemaRequirement("bearerAuth", new SecurityScheme()
//                        .name("Authorization")
//                        .type(SecurityScheme.Type.HTTP)
//                        .scheme("bearer")
//                        .bearerFormat("JWT"));
//    }
//}