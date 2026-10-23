package com.example.personas.model;
import jakarta.persistence.*;
@Entity
@Table(name = "direccion")
public class Direccion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 200) private String detalle;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "distrito_id", nullable = false) private Distrito distrito;
    protected Direccion() {}
    public Direccion(String detalle, Distrito distrito) { actualizar(detalle, distrito); }
    public void actualizar(String detalle, Distrito distrito) { this.detalle = detalle; this.distrito = distrito; }
    public Long getId() { return id; }
    public String getDetalle() { return detalle; }
    public Distrito getDistrito() { return distrito; }
}
