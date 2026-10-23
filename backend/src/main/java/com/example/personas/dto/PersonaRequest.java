package com.example.personas.dto;

import jakarta.validation.constraints.*;

public record PersonaRequest(
    @NotNull @Pattern(regexp = "[0-9]{8}", message = "El DNI debe tener 8 dígitos.") String dni,
    @NotBlank @Size(max = 100) String nombres,
    @NotBlank @Size(max = 100) String apellidos,
    @NotBlank @Size(max = 200) String direccion,
    @NotNull @Positive Long profesionId,
    @NotNull @Pattern(regexp = "9[0-9]{8}", message = "El celular debe tener 9 dígitos y empezar con 9.") String celular,
    @NotBlank @Size(max = 180) @Email @Pattern(regexp = "[^\\s@]+@[^\\s@]+\\.[^\\s@]+", message = "Ingresa un correo válido.") String correo
) {
    public PersonaRequest {
        nombres = limpiar(nombres); apellidos = limpiar(apellidos); direccion = limpiar(direccion); correo = limpiar(correo);
    }
    private static String limpiar(String valor) { return valor == null ? null : valor.trim(); }
}
