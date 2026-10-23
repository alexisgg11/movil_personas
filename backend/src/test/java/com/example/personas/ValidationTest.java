package com.example.personas;

import com.example.personas.dto.PersonaRequest;
import com.example.personas.service.PersonaService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ValidationTest {
    @Test void professionKeyIgnoresAccentsCaseAndExtraSpaces() {
        assertEquals("ingenieria de sistemas", PersonaService.claveProfesion("  INGENIERÍA   de Sistemas  "));
    }
    @Test void personRequiresEightDigitDniAndPeruvianCellphone() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(persona("01234567", "987654321", "alexis@correo.com")).isEmpty());
            assertFalse(validator.validate(persona("1234567", "987654321", "alexis@correo.com")).isEmpty());
            assertFalse(validator.validate(persona("01234567", "887654321", "alexis@correo.com")).isEmpty());
            assertFalse(validator.validate(persona("01234567", "987654321", "alexis@correo")).isEmpty());
        }
    }
    private PersonaRequest persona(String dni, String celular, String correo) {
        return new PersonaRequest(dni, "Alexis", "Sandoval", "Piura", 1L, celular, correo);
    }
}
