package com.tienda.tpv.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiTpv() {
        return new OpenAPI().info(new Info()
                .title("Alimentación Miguel — API")
                .description("API REST del punto de venta e inventario para tienda de alimentación")
                .version("v1"));
    }
}
