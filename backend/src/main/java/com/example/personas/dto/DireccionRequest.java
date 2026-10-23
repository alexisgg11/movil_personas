package com.example.personas.dto;
import jakarta.validation.constraints.*;
public record DireccionRequest(
    @NotBlank(message = "Ingresa la vía, número y demás datos de la dirección.") @Size(max = 200) String detalle,
    @NotNull @Pattern(regexp = "[0-9]{6}", message = "Selecciona un distrito válido.") String distritoId
) {
    public DireccionRequest { detalle = detalle == null ? null : detalle.trim(); }
}
