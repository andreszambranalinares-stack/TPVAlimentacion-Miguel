package com.tienda.tpv.excepciones;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ManejadorGlobalErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalErrores.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorRespuesta noEncontrado(RecursoNoEncontradoException ex) {
        return new ErrorRespuesta("NO_ENCONTRADO", ex.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorRespuesta conflicto(ConflictoException ex) {
        return new ErrorRespuesta("CONFLICTO", ex.getMessage());
    }

    @ExceptionHandler(StockInsuficienteException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorRespuesta stockInsuficiente(StockInsuficienteException ex) {
        return new ErrorRespuesta("STOCK_INSUFICIENTE", ex.getMessage());
    }

    @ExceptionHandler(ValidacionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorRespuesta validacion(ValidacionException ex) {
        return new ErrorRespuesta("VALIDACION", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorRespuesta validacionCampos(MethodArgumentNotValidException ex) {
        Map<String, String> detalles = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            detalles.put(error.getField(), error.getDefaultMessage());
        }
        return new ErrorRespuesta("VALIDACION", "Los datos enviados no son válidos", detalles);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorRespuesta cuerpoIlegible(HttpMessageNotReadableException ex) {
        return new ErrorRespuesta("VALIDACION", "El cuerpo de la petición no es válido");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorRespuesta integridad(DataIntegrityViolationException ex) {
        return new ErrorRespuesta("CONFLICTO", "La operación viola restricciones de integridad de datos");
    }

    /**
     * Las comprobaciones de @PreAuthorize (p. ej. AJUSTE reservado a ADMIN) lanzan
     * AccessDeniedException durante el dispatch normal de MVC, así que las resuelve
     * este @RestControllerAdvice antes de que lleguen al accessDeniedHandler de
     * SeguridadConfig (pensado para las reglas de authorizeHttpRequests). Devolvemos
     * el mismo formato para que el frontend no tenga que distinguir el origen del 403.
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorRespuesta accesoDenegado(AccessDeniedException ex) {
        return new ErrorRespuesta("SIN_PERMISOS", "No tienes permisos para esta operación");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorRespuesta conflictoConcurrencia(ObjectOptimisticLockingFailureException ex) {
        return new ErrorRespuesta("CONFLICTO_CONCURRENCIA",
                "Otro cambio ha modificado este producto al mismo tiempo. Vuelve a intentarlo.");
    }

    /**
     * Una dirección que no existe es un 404 normal y corriente, no un fallo del
     * servidor: sin esto acababa en el manejador genérico de abajo, que
     * respondía 500 y llenaba el registro de trazas alarmantes cada vez que
     * alguien escribía mal una URL.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorRespuesta rutaNoEncontrada(NoResourceFoundException ex) {
        return new ErrorRespuesta("NO_ENCONTRADO", "La dirección solicitada no existe");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorRespuesta errorInterno(Exception ex) {
        log.error("Error no controlado", ex);
        return new ErrorRespuesta("ERROR_INTERNO", "Se ha producido un error inesperado");
    }
}
