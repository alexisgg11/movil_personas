package com.example.personas.model;
import jakarta.persistence.*;
@Entity
@Table(name = "region")
public class Region {
    @Id @Column(length = 2) private String id;
    @Column(nullable = false, length = 100) private String nombre;
    protected Region() {}
    public String getId() { return id; }
    public String getNombre() { return nombre; }
}
