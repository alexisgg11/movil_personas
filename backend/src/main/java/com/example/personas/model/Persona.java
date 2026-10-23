package com.example.personas.model;

import jakarta.persistence.*;

@Entity
@Table(name = "persona")
public class Persona {
    @Id @Column(length = 8) private String dni;
    @Column(nullable = false, length = 100) private String nombres;
    @Column(nullable = false, length = 100) private String apellidos;
    @Column(nullable = false, length = 200) private String direccion;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profesion_id", nullable = false) private Profesion profesion;
    @Column(nullable = false, length = 9) private String celular;
    @Column(nullable = false, length = 180) private String correo;
    @Column(name = "foto_ruta", nullable = false, length = 80) private String fotoRuta;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8) private EstadoPersona estado = EstadoPersona.ACTIVO;
    protected Persona() {}
    public Persona(String dni) { this.dni = dni; }
    public void actualizar(String nombres, String apellidos, String direccion, Profesion profesion,
                           String celular, String correo, String foto) {
        this.nombres = nombres; this.apellidos = apellidos; this.direccion = direccion;
        this.profesion = profesion; this.celular = celular; this.correo = correo; fotoRuta = foto;
    }
    public void eliminar() { estado = EstadoPersona.INACTIVO; }
    public EstadoPersona getEstado() { return estado; }
    public String getDni() { return dni; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getDireccion() { return direccion; }
    public Profesion getProfesion() { return profesion; }
    public String getCelular() { return celular; }
    public String getCorreo() { return correo; }
    public String getFotoRuta() { return fotoRuta; }
}
