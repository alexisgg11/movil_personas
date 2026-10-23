package com.example.personas.repository;
import com.example.personas.model.Distrito;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface DistritoRepository extends JpaRepository<Distrito, String> {
    List<Distrito> findByProvinciaIdOrderByNombreAsc(String provinciaId);
}
