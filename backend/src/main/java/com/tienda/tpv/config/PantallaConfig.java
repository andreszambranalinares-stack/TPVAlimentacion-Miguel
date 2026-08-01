package com.tienda.tpv.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Sirve la pantalla (el frontend ya compilado) desde el propio motor, para que
 * en la tienda haya UN solo programa en marcha en vez de dos y no haga falta
 * tener Node instalado.
 *
 * <p>La pantalla se empaqueta dentro del jar en {@code /static} al compilar con
 * el perfil {@code completo} (ver pom.xml). En desarrollo no está, y entonces
 * esta configuración no hace nada: el frontend se sigue sirviendo aparte con
 * {@code npm run dev}.
 *
 * <p>Como la pantalla es una SPA (React Router), las rutas tipo {@code /caja} o
 * {@code /productos} no son ficheros reales: si el navegador las pide
 * directamente (al recargar con F5, o desde un acceso directo) hay que
 * devolverle {@code index.html} y dejar que React resuelva la ruta.
 */
@Configuration
public class PantallaConfig implements WebMvcConfigurer {

    private static final ClassPathResource INDICE = new ClassPathResource("/static/index.html");

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registro) {
        registro.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String rutaSolicitada, Resource ubicacion) throws IOException {
                        Resource fichero = ubicacion.createRelative(rutaSolicitada);
                        if (fichero.exists() && fichero.isReadable()) {
                            return fichero;
                        }
                        return esRutaDePantalla(rutaSolicitada) && INDICE.exists() ? INDICE : null;
                    }
                });
    }

    /**
     * Solo las rutas de la pantalla caen en index.html. La API y la
     * documentación técnica se quedan fuera: si alguien pide un endpoint que no
     * existe debe recibir un 404 de verdad, no la página web (que además
     * llegaría con un engañoso 200 OK).
     */
    private static boolean esRutaDePantalla(String ruta) {
        return !ruta.startsWith("api/")
                && !ruta.startsWith("api-docs")
                && !ruta.startsWith("swagger-ui");
    }
}
