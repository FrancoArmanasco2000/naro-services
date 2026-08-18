package com.naro.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Body enviado a user-service (POST /api/usuarios) al registrar una cuenta.
 * Espejo local del DTO de user-service — son módulos Maven independientes,
 * sin código compartido entre servicios.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearUsuarioRequest {

    private Long id;
    private String nombre;
    private String apellido;
    private String dni;
    private LocalDate fechaNacimiento;
    private String genero;
    private String email;
    private String ciudad;
    private String provincia;
    private String pais;

}
