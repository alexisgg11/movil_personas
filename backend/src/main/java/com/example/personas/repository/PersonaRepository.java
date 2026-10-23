package com.example.personas.repository;
import com.example.personas.model.Persona;
import com.example.personas.model.EstadoPersona;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface PersonaRepository extends JpaRepository<Persona, String> {
    @EntityGraph(attributePaths = "profesion") Optional<Persona> findByDniAndEstado(String dni, EstadoPersona estado);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Persona p where p.dni = :dni and p.estado = :estado")
    Optional<Persona> bloquear(@Param("dni") String dni, @Param("estado") EstadoPersona estado);
}
