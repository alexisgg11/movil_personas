package com.example.personas.model;
import jakarta.persistence.*;
@Entity
@Table(name = "distrito")
public class Distrito {
    @Id @Column(length = 6) private String id;
    @Column(nullable = false, length = 100) private String nombre;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provincia_id", nullable = false) private Provincia provincia;
    protected Distrito() {}
    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public Provincia getProvincia() { return provincia; }
}
