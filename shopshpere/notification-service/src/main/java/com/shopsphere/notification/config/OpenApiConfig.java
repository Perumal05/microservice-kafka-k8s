package com.shopsphere.notification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ShopSphere Notification Service API")
                        .description("Multi-channel Notification Microservice for ShopSphere (Email, SMS, Push).")
                        .version("1.0.0"));
    }
}
