package com.example.personas.controller;

import com.example.personas.service.ApiError;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    public record ErrorResponse(String mensaje, Map<String, String> campos) {}
    @ExceptionHandler(ApiError.class) ResponseEntity<ErrorResponse> api(ApiError e) { return respuesta(e.getStatus(), e.getMessage()); }
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error -> campos.putIfAbsent(error.getField().split("\\.", 2)[0], error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErrorResponse("Revisa los datos del formulario.", campos));
    }
    @ExceptionHandler({DataIntegrityViolationException.class, org.hibernate.exception.ConstraintViolationException.class})
    ResponseEntity<ErrorResponse> integridad(Exception e) { return respuesta(409, "El DNI o la profesión ya están registrados. Revisa los datos."); }
    @ExceptionHandler(MaxUploadSizeExceededException.class) ResponseEntity<ErrorResponse> grande(Exception e) { return respuesta(413, "La fotografía supera los 10 MB."); }
    @ExceptionHandler({MissingServletRequestPartException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ErrorResponse> formato(Exception e) { return respuesta(400, "Completa los datos y añade una fotografía válida."); }
    private ResponseEntity<ErrorResponse> respuesta(int status, String mensaje) { return ResponseEntity.status(status).body(new ErrorResponse(mensaje, Map.of())); }
}
