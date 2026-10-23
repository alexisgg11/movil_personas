package com.example.personas.service;
import com.example.personas.dto.UbigeoResponse;
import com.example.personas.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
@Transactional(readOnly = true)
public class UbigeoService {
    private final RegionRepository regiones;
    private final ProvinciaRepository provincias;
    private final DistritoRepository distritos;
    public UbigeoService(RegionRepository regiones, ProvinciaRepository provincias, DistritoRepository distritos) {
        this.regiones = regiones; this.provincias = provincias; this.distritos = distritos;
    }
    public List<UbigeoResponse> regiones() {
        return regiones.findAllByOrderByNombreAsc().stream().map(r -> new UbigeoResponse(r.getId(), r.getNombre())).toList();
    }
    public List<UbigeoResponse> provincias(String regionId) {
        if (!regiones.existsById(regionId)) throw new ApiError(404, "Región no encontrada.");
        return provincias.findByRegionIdOrderByNombreAsc(regionId).stream().map(p -> new UbigeoResponse(p.getId(), p.getNombre())).toList();
    }
    public List<UbigeoResponse> distritos(String provinciaId) {
        if (!provincias.existsById(provinciaId)) throw new ApiError(404, "Provincia no encontrada.");
        return distritos.findByProvinciaIdOrderByNombreAsc(provinciaId).stream().map(d -> new UbigeoResponse(d.getId(), d.getNombre())).toList();
    }
}
