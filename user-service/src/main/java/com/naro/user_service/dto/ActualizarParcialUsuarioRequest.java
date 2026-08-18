package com.naro.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import lombok.Data;

import java.time.LocalDate;

/**
 * Actualización parcial (PATCH). Todos los campos son opcionales: solo se
 * pisan los que vienen no-nulos en el request. "miembroDesde" no aparece:
 * nunca se toca. Las anotaciones de validación solo disparan si el campo
 * viene con un valor (null pasa sin chequear, sigue siendo opcional).
 */
@Data
public class ActualizarParcialUsuarioRequest {

    private String nombre;
    private String apellido;
    private String dni;
    @Past
    private LocalDate fechaNacimiento;
    private String genero;
    @Email
    private String email;
    private String telefono;
    private String ciudad;
    private String provincia;
    private String pais;

}
