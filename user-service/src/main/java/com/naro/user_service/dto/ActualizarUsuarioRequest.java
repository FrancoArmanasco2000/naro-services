package com.naro.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Data;

import java.time.LocalDate;

/**
 * Reemplazo completo del usuario (PUT). Todos los campos son obligatorios
 * salvo los explícitamente opcionales en el entity (telefono/ciudad/etc).
 * "miembroDesde" no aparece: nunca se reemplaza.
 */
@Data
public class ActualizarUsuarioRequest {

    @NotBlank
    private String nombre;
    @NotBlank
    private String apellido;
    @NotBlank
    private String dni;
    @NotNull
    @Past
    private LocalDate fechaNacimiento;
    @NotBlank
    private String genero;
    @NotBlank
    @Email
    private String email;
    private String telefono;
    private String ciudad;
    private String provincia;
    private String pais;

}
