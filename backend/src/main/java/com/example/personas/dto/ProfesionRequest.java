package com.example.personas.dto;
import jakarta.validation.constraints.*;
public record ProfesionRequest(@NotBlank @Size(max = 80) String nombre) {}
