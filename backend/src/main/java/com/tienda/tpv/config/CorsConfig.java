package com.tienda.tpv.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Permite al frontend de desarrollo (Vite) llamar a la API. */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // localhost y 127.0.0.1 son el mismo servidor para un navegador, pero orígenes
                // distintos para CORS: se permiten ambos para que el login no falle según
                // cómo se haya abierto el frontend de desarrollo.
                .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowCredentials(true);
    }
}
