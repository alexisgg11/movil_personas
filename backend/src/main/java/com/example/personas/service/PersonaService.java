package com.example.personas.service;

import com.example.personas.dto.*;
import com.example.personas.model.*;
import com.example.personas.repository.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.text.Normalizer;
import java.util.*;

@Service
@Transactional
public class PersonaService {
    private final PersonaRepository personas;
    private final ProfesionRepository profesiones;
    private final FotoStorage fotos;
    private final EntityManager em;
    public PersonaService(PersonaRepository personas, ProfesionRepository profesiones, FotoStorage fotos, EntityManager em) {
        this.personas = personas; this.profesiones = profesiones; this.fotos = fotos; this.em = em;
    }
    @Transactional(readOnly = true) public PersonaResponse buscar(String dni) { return PersonaResponse.de(activa(dni)); }
    @Transactional(readOnly = true) public boolean existe(String dni) { validarDni(dni); return personas.existsById(dni); }
    @Transactional(readOnly = true) public byte[] foto(String dni) { return fotos.leer(activa(dni).getFotoRuta()); }
    @Transactional(readOnly = true) public List<ProfesionResponse> profesiones() {
        return profesiones.findAllByOrderByNombreAsc().stream().map(ProfesionResponse::de).toList();
    }
    public ProfesionResponse crearProfesion(String nombre) {
        String limpio = nombre.trim().replaceAll("\\s+", " ");
        String clave = claveProfesion(limpio);
        if (clave.isBlank()) throw new ApiError(400, "Ingresa el nombre de la profesión.");
        if (profesiones.existsByNombreClave(clave)) throw new ApiError(409, "La profesión ya está registrada.");
        return ProfesionResponse.de(profesiones.saveAndFlush(new Profesion(limpio, clave)));
    }
    public PersonaResponse crear(PersonaRequest datos, MultipartFile foto) {
        if (personas.existsById(datos.dni())) throw new ApiError(409, "Este DNI ya está registrado, incluso si fue eliminado.");
        Profesion profesion = profesion(datos.profesionId());
        Persona persona = new Persona(datos.dni());
        aplicarDatos(persona, datos, profesion, fotos.guardar(datos.dni(), foto, null));
        em.persist(persona); em.flush();
        return PersonaResponse.de(persona);
    }
    public PersonaResponse actualizar(String dni, PersonaRequest datos, MultipartFile foto) {
        validarDni(dni);
        if (!dni.equals(datos.dni())) throw new ApiError(400, "El DNI de un registro no se puede cambiar.");
        Persona persona = personas.bloquear(dni, EstadoPersona.ACTIVO).orElseThrow(() -> new ApiError(404, "Persona no encontrada."));
        Profesion profesion = profesion(datos.profesionId());
        String ruta = foto == null || foto.isEmpty() ? persona.getFotoRuta() : fotos.guardar(dni, foto, persona.getFotoRuta());
        aplicarDatos(persona, datos, profesion, ruta); em.flush();
        return PersonaResponse.de(persona);
    }
    public void eliminar(String dni) {
        validarDni(dni);
        personas.bloquear(dni, EstadoPersona.ACTIVO).orElseThrow(() -> new ApiError(404, "Persona no encontrada.")).eliminar();
    }
    private Persona activa(String dni) {
        validarDni(dni); return personas.findByDniAndEstado(dni, EstadoPersona.ACTIVO).orElseThrow(() -> new ApiError(404, "No se encontró una persona con ese DNI."));
    }
    private void aplicarDatos(Persona persona, PersonaRequest d, Profesion profesion, String foto) {
        persona.actualizar(d.nombres(), d.apellidos(), d.direccion(), profesion, d.celular(), d.correo(), foto);
    }
    private Profesion profesion(Long id) { return profesiones.findById(id).orElseThrow(() -> new ApiError(400, "Selecciona una profesión existente.")); }
    private void validarDni(String dni) { if (!dni.matches("[0-9]{8}")) throw new ApiError(400, "El DNI debe tener 8 dígitos."); }
    public static String claveProfesion(String nombre) {
        return Normalizer.normalize(nombre.trim().replaceAll("\\s+", " "), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
