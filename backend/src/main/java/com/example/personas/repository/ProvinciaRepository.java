package com.example.personas.repository;
import com.example.personas.model.Provincia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ProvinciaRepository extends JpaRepository<Provincia, String> {
    List<Provincia> findByRegionIdOrderByNombreAsc(String regionId);
}
