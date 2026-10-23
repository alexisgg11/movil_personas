package com.example.personas.repository;
import com.example.personas.model.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RegionRepository extends JpaRepository<Region, String> {
    List<Region> findAllByOrderByNombreAsc();
}
