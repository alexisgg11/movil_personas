package com.example.personas.controller;

import com.example.personas.dto.*;
import com.example.personas.service.PersonaService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@RestController
@RequestMapping("/api")
public class PersonaController {
    private final PersonaService service;
    private final JdbcTemplate jdbc;
    public PersonaController(PersonaService service, JdbcTemplate jdbc) { this.service = service; this.jdbc = jdbc; }
    @GetMapping("/health") public Map<String, String> health() {
        jdbc.queryForObject("SELECT 1", Integer.class);
        return Map.of("estado", "ok", "baseDatos", "PostgreSQL", "aplicacion", "personas",
            "instancia", System.getProperty("app.instance", "manual"), "version", System.getProperty("app.build", "manual"));
    }
    @GetMapping("/profesiones") public List<ProfesionResponse> profesiones() { return service.profesiones(); }
    @PostMapping("/profesiones") @ResponseStatus(HttpStatus.CREATED)
    public ProfesionResponse crearProfesion(@Valid @RequestBody ProfesionRequest datos) { return service.crearProfesion(datos.nombre()); }
    @GetMapping("/personas/{dni}") public PersonaResponse buscar(@PathVariable String dni) { return service.buscar(dni); }
    @GetMapping("/personas/{dni}/existe") public Map<String, Boolean> existe(@PathVariable String dni) { return Map.of("existe", service.existe(dni)); }
    @GetMapping("/personas/{dni}/foto") public ResponseEntity<byte[]> foto(@PathVariable String dni) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_JPEG).body(service.foto(dni));
    }
    @PostMapping(value = "/personas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
    public PersonaResponse crear(@Valid @RequestPart("datos") PersonaRequest datos, @RequestPart("foto") MultipartFile foto) { return service.crear(datos, foto); }
    @PutMapping(value = "/personas/{dni}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PersonaResponse actualizar(@PathVariable String dni, @Valid @RequestPart("datos") PersonaRequest datos,
        @RequestPart(value = "foto", required = false) MultipartFile foto) { return service.actualizar(dni, datos, foto); }
    @DeleteMapping("/personas/{dni}") @ResponseStatus(HttpStatus.NO_CONTENT) public void eliminar(@PathVariable String dni) { service.eliminar(dni); }
}
