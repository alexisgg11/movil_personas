package com.example.personas.dto;
import com.example.personas.model.Profesion;
public record ProfesionResponse(Long id, String nombre) {
    public static ProfesionResponse de(Profesion p) { return new ProfesionResponse(p.getId(), p.getNombre()); }
}
