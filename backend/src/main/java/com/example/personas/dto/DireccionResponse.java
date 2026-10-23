package com.example.personas.dto;
import com.example.personas.model.Direccion;
public record DireccionResponse(Long id, String detalle, String distritoId, String distrito,
    String provinciaId, String provincia, String regionId, String region) {
    public static DireccionResponse de(Direccion d) {
        var distrito = d.getDistrito();
        var provincia = distrito.getProvincia();
        var region = provincia.getRegion();
        return new DireccionResponse(d.getId(), d.getDetalle(), distrito.getId(), distrito.getNombre(),
            provincia.getId(), provincia.getNombre(), region.getId(), region.getNombre());
    }
}
