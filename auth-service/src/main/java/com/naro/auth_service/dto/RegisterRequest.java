package com.naro.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {

    @NotBlank
    private String nombre;
    @NotBlank
    private String apellido;
    @NotBlank
    private String dni;
    @NotNull
    private LocalDate fechaNacimiento;
    @NotBlank
    private String genero;
    // Ubicacion: opcional, se puede completar despues desde el perfil.
    private String ciudad;
    private String provincia;
    private String pais;
    @NotBlank
    @Email
    private String email;
    @NotBlank
    @Size(min = 8)
    private String password;
    @NotBlank
    @Size(min = 8)
    private String confirmPassword;

}
