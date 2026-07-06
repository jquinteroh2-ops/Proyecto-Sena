package com.educktrack.usuarios.infrastructure.rest;

import com.educktrack.usuarios.domain.NombreRol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Datos para registrar una cuenta de usuario (RF-01, HU-01).
 */
public record RegistrarUsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotBlank(message = "El correo institucional es obligatorio")
        @Email(message = "El correo no tiene un formato valido")
        String correo,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres")
        String password,

        @NotEmpty(message = "Debe asignar al menos un rol")
        List<NombreRol> roles) {
}
