package com.example.personas.dto;
import com.example.personas.model.Persona;
public record PersonaResponse(String dni, String nombres, String apellidos, DireccionResponse direccion,
    Long profesionId, String profesion, String celular, String correo, String fotoUrl) {
    public static PersonaResponse de(Persona p) {
        return new PersonaResponse(p.getDni(), p.getNombres(), p.getApellidos(), DireccionResponse.de(p.getDireccion()),
            p.getProfesion().getId(), p.getProfesion().getNombre(), p.getCelular(), p.getCorreo(),
            "/api/personas/" + p.getDni() + "/foto?v=" + p.getFotoRuta());
    }
}
