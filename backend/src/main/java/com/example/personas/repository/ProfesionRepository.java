package com.example.personas.repository;
import com.example.personas.model.Profesion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ProfesionRepository extends JpaRepository<Profesion, Long> {
    List<Profesion> findAllByOrderByNombreAsc();
    boolean existsByNombreClave(String clave);
}
