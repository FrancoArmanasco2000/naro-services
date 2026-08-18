package com.naro.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CrearUsuarioRequest {

    @NotNull
    private Long id;
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
