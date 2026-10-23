package com.example.personas.controller;
import com.example.personas.dto.UbigeoResponse;
import com.example.personas.service.UbigeoService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/ubigeo")
public class UbigeoController {
    private final UbigeoService service;
    public UbigeoController(UbigeoService service) { this.service = service; }
    @GetMapping("/regiones") public List<UbigeoResponse> regiones() { return service.regiones(); }
    @GetMapping("/regiones/{id}/provincias") public List<UbigeoResponse> provincias(@PathVariable String id) { return service.provincias(id); }
    @GetMapping("/provincias/{id}/distritos") public List<UbigeoResponse> distritos(@PathVariable String id) { return service.distritos(id); }
}
