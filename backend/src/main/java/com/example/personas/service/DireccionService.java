package com.example.personas.service;
import com.example.personas.dto.DireccionRequest;
import com.example.personas.model.Direccion;
import com.example.personas.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional
public class DireccionService {
    private final DireccionRepository direcciones;
    private final DistritoRepository distritos;
    public DireccionService(DireccionRepository direcciones, DistritoRepository distritos) {
        this.direcciones = direcciones; this.distritos = distritos;
    }
    public Direccion guardar(DireccionRequest datos, Direccion anterior) {
        var distrito = distritos.findById(datos.distritoId()).orElseThrow(() -> new ApiError(400, "Selecciona un distrito existente."));
        if (anterior == null) return direcciones.save(new Direccion(datos.detalle(), distrito));
        anterior.actualizar(datos.detalle(), distrito);
        return anterior;
    }
}
