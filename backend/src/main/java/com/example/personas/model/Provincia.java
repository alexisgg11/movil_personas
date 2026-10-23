package com.example.personas.model;
import jakarta.persistence.*;
@Entity
@Table(name = "provincia")
public class Provincia {
    @Id @Column(length = 4) private String id;
    @Column(nullable = false, length = 100) private String nombre;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false) private Region region;
    protected Provincia() {}
    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public Region getRegion() { return region; }
}
