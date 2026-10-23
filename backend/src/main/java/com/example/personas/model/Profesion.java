package com.example.personas.model;

import jakarta.persistence.*;

@Entity
@Table(name = "profesion")
public class Profesion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 80) private String nombre;
    @Column(name = "nombre_clave", nullable = false, unique = true, length = 80) private String nombreClave;
    protected Profesion() {}
    public Profesion(String nombre, String clave) { this.nombre = nombre; this.nombreClave = clave; }
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
}
