package com.tienda.tpv.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Seguridad de la API con sesión de servidor:
 * - /api/auth/** y Swagger son públicos
 * - la gestión (escrituras de catálogo, informes y cierres) exige rol ADMIN
 * - el resto (caja, consultas, movimientos de stock) exige estar autenticado
 */
@Configuration
@EnableWebSecurity
public class SeguridadConfig {

    @Bean
    public SecurityFilterChain cadenaDeFiltros(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {
                })
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/error").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/api-docs", "/api-docs/**").permitAll()
                        .requestMatchers("/api/informes/**", "/api/cierres-caja/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/productos/**", "/api/categorias/**", "/api/proveedores/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/productos/**", "/api/categorias/**", "/api/proveedores/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/productos/**", "/api/categorias/**", "/api/proveedores/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint((peticion, respuesta, ex) ->
                                escribirError(respuesta, HttpServletResponse.SC_UNAUTHORIZED,
                                        "NO_AUTENTICADO", "Debes iniciar sesión"))
                        .accessDeniedHandler((peticion, respuesta, ex) ->
                                escribirError(respuesta, HttpServletResponse.SC_FORBIDDEN,
                                        "SIN_PERMISOS", "No tienes permisos para esta operación")));
        return http.build();
    }

    private void escribirError(HttpServletResponse respuesta, int estado, String codigo, String mensaje)
            throws java.io.IOException {
        respuesta.setStatus(estado);
        respuesta.setContentType("application/json;charset=UTF-8");
        respuesta.getWriter().write("{\"codigo\":\"" + codigo + "\",\"mensaje\":\"" + mensaje + "\"}");
    }

    @Bean
    public PasswordEncoder codificadorPasswords() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager gestorAutenticacion(AuthenticationConfiguration configuracion) throws Exception {
        return configuracion.getAuthenticationManager();
    }

    @Bean
    public SecurityContextRepository repositorioContextoSeguridad() {
        return new HttpSessionSecurityContextRepository();
    }
}
