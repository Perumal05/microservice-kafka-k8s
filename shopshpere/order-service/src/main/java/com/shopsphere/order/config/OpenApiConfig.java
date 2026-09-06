package com.shopsphere.order.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ShopSphere Order Service API")
                        .description("Production-style Order Service RESTful API for ShopSphere e-commerce platform.")
                        .version("1.0.0"));
    }
}
